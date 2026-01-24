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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepo poRepository;
    private final PurchaseOrderItemRepo poItemRepository;
    private final ItemsRepo itemsRepo;

    // 1️⃣ Add or Update Purchase Order
    @Transactional
    public PurchaseOrder createOrUpdatePO(PurchaseOrder poInput, String loggedInUser) {
        LocalDateTime now = LocalDateTime.now();

        if (poInput.getId() != null) {
            // Update existing PO
            PurchaseOrder existing = poRepository.findById(poInput.getId())
                    .orElseThrow(() -> new RuntimeException("PO not found"));
            existing.setProjectId(poInput.getProjectId());
            existing.setVendorId(poInput.getVendorId());
            existing.setUpdatedBy(loggedInUser);
            existing.setUpdatedDate(now);
            existing.setStatus(poInput.getStatus() != null ? poInput.getStatus() : existing.getStatus());
            return poRepository.save(existing);
        } else {
            // Create new PO
            poInput.setCreatedBy(loggedInUser);
            poInput.setUpdatedBy(loggedInUser);
            poInput.setCreatedDate(now);
            poInput.setUpdatedDate(now);
            poInput.setStatus(poInput.getStatus() != null ? poInput.getStatus() : PoStatus.OPEN);
            poInput.setPoNumber(generatePONumber());
            // TODO: generate poNumber here if needed
            return poRepository.save(poInput);
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

            // 2️⃣ Update PO totalAmount
            po.setTotalAmount(totalAmount);
            po.setUpdatedBy(loggedInUser);
            po.setUpdatedDate(now);
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
