package com.rem.backend.purchasemanagement.service;

import com.rem.backend.purchasemanagement.entity.items.Items;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrder;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrderItem;
import com.rem.backend.purchasemanagement.enums.PoStatus;
import com.rem.backend.purchasemanagement.repository.ItemsRepo;
import com.rem.backend.purchasemanagement.repository.PurchaseOrderItemRepo;
import com.rem.backend.purchasemanagement.repository.PurchaseOrderRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepo poRepository;
    private final PurchaseOrderItemRepo poItemRepository;
    private final ItemsRepo itemsRepo;

    // 1️⃣ Add or Update Purchase Order
    @Transactional
    public Map<String, Object> createOrUpdatePO(PurchaseOrder poInput, String loggedInUser) {

        LocalDateTime now = LocalDateTime.now();
        PurchaseOrder po;

        try {

            // ===========================
            // 1️⃣ Basic PO Validations
            // ===========================
            ValidationService.validate(poInput.getOrgId(), "Organization Id");
            ValidationService.validate(poInput.getProjectId(), "Project Id");
            ValidationService.validate(poInput.getVendorId(), "Vendor Id");

            // ===========================
            // 2️⃣ Items List Validation
            // ===========================
            if (poInput.getPurchaseOrderItemList() == null || poInput.getPurchaseOrderItemList().isEmpty()) {
                throw new IllegalArgumentException("Purchase order must contain at least one item");
            }

            // ===========================
            // 3️⃣ Update / Create PO
            // ===========================
            if (poInput.getId() != null) {

                po = poRepository.findById(poInput.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found"));

                po.setProjectId(poInput.getProjectId());
                po.setVendorId(poInput.getVendorId());
                po.setUpdatedBy(loggedInUser);
                po.setUpdatedDate(now);
                po.setStatus(poInput.getStatus() != null ? poInput.getStatus() : po.getStatus());

            } else {

                po = poInput;
                po.setCreatedBy(loggedInUser);
                po.setUpdatedBy(loggedInUser);
                po.setCreatedDate(now);
                po.setUpdatedDate(now);
                po.setStatus(poInput.getStatus() != null ? poInput.getStatus() : PoStatus.OPEN);
                po.setPoNumber(generatePONumber());
            }

            // 4️⃣ Save PO first
            po = poRepository.save(po);

            // If updating, load existing PO items to detect deletes
            Map<Long, PurchaseOrderItem> existingItemsMap = new HashMap<>();
            Set<Long> processedExistingIds = new HashSet<>();
            if (poInput.getId() != null) {
                List<PurchaseOrderItem> existingItems = poItemRepository.findAllByPoId(po.getId());
                for (PurchaseOrderItem existing : existingItems) {
                    if (existing.getId() != null) {
                        existingItemsMap.put(existing.getId(), existing);
                    }
                }
            }

            // ===========================
            // 5️⃣ Items Validation + Save (add/update)
            // ===========================
            double totalAmount = 0;
            Set<Long> uniqueItemIds = new HashSet<>();

            for (PurchaseOrderItem itemInput : poInput.getPurchaseOrderItemList()) {

                // Field validations
                ValidationService.validate(itemInput.getItemsId(), "Item Id");
                ValidationService.validate(itemInput.getQuantity(), "Quantity");
                ValidationService.validate(itemInput.getRate(), "Rate");

                if (itemInput.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Quantity must be greater than 0");
                }

                if (itemInput.getRate() <= 0) {
                    throw new IllegalArgumentException("Rate must be greater than 0");
                }

                // Duplicate item validation
                if (!uniqueItemIds.add(itemInput.getItemsId())) {
                    throw new IllegalArgumentException("Duplicate item found in purchase order (Item ID: " + itemInput.getItemsId() + ")");
                }

                Items item = itemsRepo.findById(itemInput.getItemsId())
                        .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + itemInput.getItemsId()));

                PurchaseOrderItem poItem;

                if (itemInput.getId() != null) {
                    // Update existing poItem
                    poItem = poItemRepository.findById(itemInput.getId())
                            .orElseThrow(() -> new IllegalArgumentException("PO Item not found with id: " + itemInput.getId()));

                    // mark as processed so it won't be deleted later
                    processedExistingIds.add(poItem.getId());
                } else {
                    // Create new poItem
                    poItem = new PurchaseOrderItem();
                    poItem.setPoId(po.getId());
                    poItem.setCreatedBy(loggedInUser);
                    poItem.setCreatedDate(now);
                }

                poItem.setItems(item);
                poItem.setQuantity(itemInput.getQuantity());
                poItem.setRate(itemInput.getRate());
                poItem.setAmount(itemInput.getQuantity() * itemInput.getRate());
                poItem.setUpdatedBy(loggedInUser);
                poItem.setUpdatedDate(now);

                poItemRepository.save(poItem);

                totalAmount += poItem.getAmount();
            }

            // ===========================
            // 6️⃣ Delete removed items (only when updating)
            // ===========================
            if (!existingItemsMap.isEmpty()) {
                for (Long existingId : existingItemsMap.keySet()) {
                    if (!processedExistingIds.contains(existingId)) {
                        // delete item removed by user
                        poItemRepository.deleteById(existingId);
                    }
                }
                // Recompute total amount from DB to ensure correctness after deletes
                List<PurchaseOrderItem> allItemsAfter = poItemRepository.findAllByPoId(po.getId());
                totalAmount = allItemsAfter.stream().mapToDouble(PurchaseOrderItem::getAmount).sum();
            }

            // ===========================
            // 7️⃣ Final PO Update
            // ===========================
            if (totalAmount <= 0) {
                throw new IllegalArgumentException("Total amount must be greater than 0");
            }

            po.setTotalAmount(totalAmount);
            po.setUpdatedBy(loggedInUser);
            po.setUpdatedDate(now);
            poRepository.save(po);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Purchase Order Created/Updated Successfully!");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }



    // 2️⃣ Add or Update PO Item
    @Transactional
    public Map<String, Object> updatePOItems(Long poId, List<PurchaseOrderItem> itemsInput, String loggedInUser) {
        LocalDateTime now = LocalDateTime.now();

        // 1️⃣ Fetch PO

        try {
            PurchaseOrder po = poRepository.findById(poId)
                    .orElseThrow(() -> new RuntimeException("PO not found"));

            // Load existing items to detect deletions
            List<PurchaseOrderItem> existingItems = poItemRepository.findAllByPoId(poId);
            Map<Long, PurchaseOrderItem> existingItemsMap = new HashMap<>();
            for (PurchaseOrderItem existing : existingItems) {
                if (existing.getId() != null) {
                    existingItemsMap.put(existing.getId(), existing);
                }
            }
            Set<Long> processedExistingIds = new HashSet<>();

            double totalAmount = 0;

            for (PurchaseOrderItem itemInput : itemsInput) {
                PurchaseOrderItem poItem;
                Items item;

                if (itemInput.getId() != null) {
                    // Update existing poItem
                    poItem = poItemRepository.findById(itemInput.getId())
                            .orElseThrow(() -> new RuntimeException("PO Item not found"));

                    item = itemsRepo.findById(itemInput.getItemsId())
                            .orElseThrow(() -> new RuntimeException("Items not found"));

                    poItem.setItems(item);
                    poItem.setQuantity(itemInput.getQuantity());
                    poItem.setRate(itemInput.getRate());
                    poItem.setAmount(itemInput.getQuantity() * itemInput.getRate());
                    poItem.setUpdatedBy(loggedInUser);
                    poItem.setUpdatedDate(now);

                    // mark as processed so it won't be deleted
                    processedExistingIds.add(poItem.getId());

                } else {
                    // Create new poItem

                    item = itemsRepo.findById(itemInput.getItemsId())
                            .orElseThrow(() -> new RuntimeException("Items not found"));

                    poItem = new PurchaseOrderItem();
                    poItem.setPoId(poId);
                    poItem.setItems(item);
                    poItem.setQuantity(itemInput.getQuantity());
                    poItem.setRate(itemInput.getRate());
                    poItem.setAmount(itemInput.getQuantity() * itemInput.getRate());
                    poItem.setCreatedBy(loggedInUser);
                    poItem.setUpdatedBy(loggedInUser);
                    poItem.setCreatedDate(now);
                    poItem.setUpdatedDate(now);
                }

                poItemRepository.save(poItem);

                // Add to total
                totalAmount += poItem.getAmount();
            }

            // Delete removed items
            if (!existingItemsMap.isEmpty()) {
                for (Long existingId : existingItemsMap.keySet()) {
                    if (!processedExistingIds.contains(existingId)) {
                        poItemRepository.deleteById(existingId);
                    }
                }
                // Recompute total after deletions
                List<PurchaseOrderItem> allItemsAfter = poItemRepository.findAllByPoId(poId);
                totalAmount = allItemsAfter.stream().mapToDouble(PurchaseOrderItem::getAmount).sum();
            }

            // 2️⃣ Update PO totalAmount and persist
            po.setTotalAmount(totalAmount);
            po.setUpdatedBy(loggedInUser);
            po.setUpdatedDate(now);
            poRepository.save(po);

            return ResponseMapper.buildResponse(Responses.SUCCESS, po);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    // 3️⃣ Calculate total amount of PO from its items
    @Transactional
    public Double calculateTotalAmount(Long poId) {
        List<PurchaseOrderItem> items = poItemRepository.findAllByPoId(poId);
        return items.stream().mapToDouble(PurchaseOrderItem::getAmount).sum();
    }

    // 4️⃣ Fetch PO with its items
    @Transactional
    public Map<String, Object> getPOWithItems(Long poId) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        List<PurchaseOrderItem> items = poItemRepository.findAllByPoId(poId);
        Map<String, Object> map = new HashMap<>();
        map.put("po", po);
        map.put("items", items);
        return map;
    }


    @Transactional
    public Map<String, Object> getById(Long poId) {

        try{
            PurchaseOrder po = poRepository.findById(poId)
                    .orElseThrow(() -> new IllegalArgumentException("Purchased Order not found"));

            List<PurchaseOrderItem> items = poItemRepository.findAllByPoId(poId);

            Map<String, Object> map = new HashMap<>();
            map.put("po", po);
            map.put("items", items);

            return ResponseMapper.buildResponse(Responses.SUCCESS , map);
        }catch (IllegalArgumentException e){
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER , e.getMessage());
        }catch (Exception e){
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER , e.getMessage());
        }
    }



    @Transactional
    public Map<String, Object> getAll(Long organizationId, Pageable pageable) {

        try{

            List<PurchaseOrder>  poList = poRepository.findByOrgId(organizationId , pageable);

            for (PurchaseOrder po : poList){

                List<PurchaseOrderItem> items = poItemRepository.findAllByPoId(po.getId());
                po.setPurchaseOrderItemList(items);

            }

            return ResponseMapper.buildResponse(Responses.SUCCESS , poList);

        }catch (IllegalArgumentException e){
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER , e.getMessage());
        }catch (Exception e){
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER , e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> getAllByStatus(Long organizationId, PoStatus status) {
        try {
            ValidationService.validate(organizationId, "Organization Id");
            ValidationService.validate(status, "PoStatus");

            List<PurchaseOrder> poList = poRepository.findByOrgIdAndStatus(organizationId, status);

            for (PurchaseOrder po : poList) {
                List<PurchaseOrderItem> items = poItemRepository.findAllByPoId(po.getId());
                po.setPurchaseOrderItemList(items);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, poList);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // 5️⃣ Close PO safely
    @Transactional
    public PurchaseOrder closePO(Long poId, String loggedInUser) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        // Optional: check pending items logic here
        po.setStatus(PoStatus.CLOSED);
        po.setUpdatedBy(loggedInUser);
        po.setUpdatedDate(LocalDateTime.now());
        return poRepository.save(po);
    }

    // 6️⃣ Approve Purchase Order
    @Transactional
    public Map<String, Object> approvePO(Long poId, String loggedInUser) {
        try {
            PurchaseOrder po = poRepository.findById(poId)
                    .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found"));

            if (po.getStatus() != PoStatus.OPEN) {
                throw new IllegalArgumentException("Only OPEN Purchase Orders can be approved");
            }

            List<PurchaseOrderItem> items = poItemRepository.findAllByPoId(poId);
            if (items == null || items.isEmpty()) {
                throw new IllegalArgumentException("Cannot approve Purchase Order without items");
            }

            // Initialize received quantities
            for (PurchaseOrderItem item : items) {
                if (item.getReceivedQuantity() == null) {
                    item.setReceivedQuantity(0.0);
                    item.setInvoicedQuantity(0.0);
                    poItemRepository.save(item);
                }
            }

            po.setStatus(PoStatus.PARTIAL); // Move to partial to indicate it's approved and awaiting GRN
            po.setUpdatedBy(loggedInUser);
            po.setUpdatedDate(LocalDateTime.now());
            poRepository.save(po);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Purchase Order approved successfully");

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // 7️⃣ Cancel/Soft Delete Purchase Order
    @Transactional
    public Map<String, Object> cancelPO(Long poId, String loggedInUser) {
        try {
            PurchaseOrder po = poRepository.findById(poId)
                    .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found"));

            if (po.getStatus() == PoStatus.CLOSED) {
                throw new IllegalArgumentException("Cannot cancel a closed Purchase Order");
            }

            // Check if any GRN exists for this PO
            // If items have been received, cannot cancel
            List<PurchaseOrderItem> items = poItemRepository.findAllByPoId(poId);
            for (PurchaseOrderItem item : items) {
                if (item.getReceivedQuantity() != null && item.getReceivedQuantity() > 0) {
                    throw new IllegalArgumentException("Cannot cancel Purchase Order with received items");
                }
            }

            po.setStatus(PoStatus.CANCELLED);
            po.setUpdatedBy(loggedInUser);
            po.setUpdatedDate(LocalDateTime.now());
            poRepository.save(po);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Purchase Order cancelled successfully");

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public String generatePONumber() {
        // Format: PO-yyyyMMdd-XXX
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Get last PO
        Optional<PurchaseOrder> lastPOOpt = poRepository.findTopByOrderByIdDesc();

        int nextSequence = 1;
        if (lastPOOpt.isPresent()) {
            String lastPoNumber = lastPOOpt.get().getPoNumber(); // e.g., "PO-20260124-005"

            // Extract the last sequence number
            String[] parts = lastPoNumber.split("-");
            if (parts.length == 3 && parts[1].equals(datePart)) {
                nextSequence = Integer.parseInt(parts[2]) + 1;
            }
        }

        return String.format("PO-%s-%03d", datePart, nextSequence);
    }

}
