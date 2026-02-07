package com.rem.backend.purchasemanagement.service;

import com.rem.backend.purchasemanagement.entity.grn.Grn;
import com.rem.backend.purchasemanagement.entity.grn.GrnItems;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrder;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrderItem;
import com.rem.backend.purchasemanagement.enums.GrnStatus;
import com.rem.backend.purchasemanagement.enums.PoStatus;
import com.rem.backend.purchasemanagement.repository.*;
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
public class GrnService {

    private final GrnRepo grnRepo;
    private final GrnItemsRepo grnItemsRepo;
    private final PurchaseOrderRepo poRepository;
    private final PurchaseOrderItemRepo poItemRepository;

    // ==================== 1. CREATE GRN FROM PURCHASE ORDER ====================
    @Transactional
    public Map<String, Object> createGrn(Grn grnInput, String loggedInUser) {

        LocalDateTime now = LocalDateTime.now();

        try {
            // ===========================
            // 1️⃣ Basic GRN Validations
            // ===========================
            ValidationService.validate(grnInput.getPoId(), "Purchase Order Id");

            if (grnInput.getGrnItemsList() == null || grnInput.getGrnItemsList().isEmpty()) {
                throw new IllegalArgumentException("GRN must contain at least one item");
            }

            // ===========================
            // 2️⃣ Fetch and Validate PO
            // ===========================
            PurchaseOrder po = poRepository.findById(grnInput.getPoId())
                    .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found"));

            if (po.getStatus() == PoStatus.CLOSED || po.getStatus() == PoStatus.CANCELLED) {
                throw new IllegalArgumentException("Cannot create GRN for closed or cancelled Purchase Order");
            }

            // ===========================
            // 3️⃣ Validate GRN Quantities against PO
            // ===========================
            List<PurchaseOrderItem> poItems = poItemRepository.findAllByPoId(po.getId());
            Map<Long, PurchaseOrderItem> poItemMap = new HashMap<>();
            for (PurchaseOrderItem item : poItems) {
                poItemMap.put(item.getId(), item);
            }

            for (GrnItems grnItem : grnInput.getGrnItemsList()) {
                ValidationService.validate(grnItem.getPoItemId(), "PO Item Id");
                ValidationService.validate(grnItem.getQuantityReceived(), "Quantity Received");

                if (grnItem.getQuantityReceived() <= 0) {
                    throw new IllegalArgumentException("Quantity received must be greater than 0");
                }

                PurchaseOrderItem poItem = poItemMap.get(grnItem.getPoItemId());
                if (poItem == null) {
                    throw new IllegalArgumentException("PO Item not found with id: " + grnItem.getPoItemId());
                }

                // Calculate total already received for this PO item
                Double alreadyReceived = grnItemsRepo.getTotalReceivedQuantityByPoItemId(grnItem.getPoItemId());
                Double pendingQuantity = poItem.getQuantity() - alreadyReceived;

                if (grnItem.getQuantityReceived() > pendingQuantity) {
                    throw new IllegalArgumentException("GRN quantity (" + grnItem.getQuantityReceived() +
                            ") exceeds pending quantity (" + pendingQuantity + ") for PO Item: " + grnItem.getPoItemId());
                }
            }

            // ===========================
            // 4️⃣ Create GRN
            // ===========================
            Grn grn = new Grn();
            grn.setGrnNumber(generateGRNNumber());
            grn.setOrgId(po.getOrgId());
            grn.setProjectId(po.getProjectId());
            grn.setVendorId(po.getVendorId());
            grn.setPoId(po.getId());
            grn.setStatus(GrnStatus.RECEIVED);
            grn.setReceivedDate(grnInput.getReceivedDate() != null ? grnInput.getReceivedDate() : now);
            grn.setCreatedBy(loggedInUser);
            grn.setUpdatedBy(loggedInUser);
            grn.setCreatedDate(now);
            grn.setUpdatedDate(now);

            grn = grnRepo.save(grn);

            // ===========================
            // 5️⃣ Save GRN Items and Update PO Item Received Quantities
            // ===========================
            for (GrnItems grnItem : grnInput.getGrnItemsList()) {
                PurchaseOrderItem poItem = poItemMap.get(grnItem.getPoItemId());

                GrnItems newGrnItem = new GrnItems();
                newGrnItem.setGrnId(grn.getId());
                newGrnItem.setPoItemId(grnItem.getPoItemId());
                newGrnItem.setItemId(poItem.getItems().getId());
                newGrnItem.setQuantityReceived(grnItem.getQuantityReceived());
                newGrnItem.setQuantityInvoiced(0.0);
                newGrnItem.setCreatedBy(loggedInUser);
                newGrnItem.setUpdatedBy(loggedInUser);
                newGrnItem.setCreatedDate(now);
                newGrnItem.setUpdatedDate(now);

                grnItemsRepo.save(newGrnItem);

                // Update received quantity in PO item
                Double currentReceived = poItem.getReceivedQuantity() != null ? poItem.getReceivedQuantity() : 0.0;
                poItem.setReceivedQuantity(currentReceived + grnItem.getQuantityReceived());
                poItem.setUpdatedBy(loggedInUser);
                poItem.setUpdatedDate(now);
                poItemRepository.save(poItem);
            }

            // ===========================
            // 6️⃣ Update PO Status based on received quantities
            // ===========================
            updatePOStatusAfterGRN(po.getId(), loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "GRN created successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 2. GET GRN BY ID ====================
    @Transactional
    public Map<String, Object> getById(Long grnId) {
        try {
            Grn grn = grnRepo.findById(grnId)
                    .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

            List<GrnItems> items = grnItemsRepo.findByGrnId(grnId);
            grn.setGrnItemsList(items);

            return ResponseMapper.buildResponse(Responses.SUCCESS, grn);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 3. GET ALL GRNs BY PO ID ====================
    @Transactional
    public Map<String, Object> getByPoId(Long poId, Pageable pageable) {
        try {
            List<Grn> grnList = grnRepo.findByPoId(poId);

            for (Grn grn : grnList) {
                List<GrnItems> items = grnItemsRepo.findByGrnId(grn.getId());
                grn.setGrnItemsList(items);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, grnList);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== HELPER: Update PO Status After GRN ====================
    private void updatePOStatusAfterGRN(Long poId, String loggedInUser) {
        List<PurchaseOrderItem> poItems = poItemRepository.findAllByPoId(poId);

        boolean allFullyReceived = true;
        boolean anyReceived = false;

        for (PurchaseOrderItem item : poItems) {
            Double received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0.0;
            if (received > 0) {
                anyReceived = true;
            }
            if (received < item.getQuantity()) {
                allFullyReceived = false;
            }
        }

        PurchaseOrder po = poRepository.findById(poId).orElseThrow();

        if (allFullyReceived) {
            po.setStatus(PoStatus.CLOSED);
        } else if (anyReceived) {
            po.setStatus(PoStatus.PARTIAL);
        }

        po.setUpdatedBy(loggedInUser);
        po.setUpdatedDate(LocalDateTime.now());
        poRepository.save(po);
    }

    // ==================== HELPER: Generate GRN Number ====================
    public String generateGRNNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Optional<Grn> lastGrnOpt = grnRepo.findTopByOrderByIdDesc();

        int nextSequence = 1;
        if (lastGrnOpt.isPresent()) {
            String lastGrnNumber = lastGrnOpt.get().getGrnNumber();
            if (lastGrnNumber != null) {
                String[] parts = lastGrnNumber.split("-");
                if (parts.length == 3 && parts[1].equals(datePart)) {
                    nextSequence = Integer.parseInt(parts[2]) + 1;
                }
            }
        }

        return String.format("GRN-%s-%03d", datePart, nextSequence);
    }
}
