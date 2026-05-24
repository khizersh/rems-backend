package com.rem.backend.purchasemanagement.service;

import com.rem.backend.purchasemanagement.dto.GrnByPoGroupedResponseDTO;
import com.rem.backend.purchasemanagement.entity.grn.Grn;
import com.rem.backend.purchasemanagement.entity.grn.GrnItems;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrder;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrderItem;
import com.rem.backend.purchasemanagement.enums.GrnStatus;
import com.rem.backend.purchasemanagement.enums.GrnInvoiceStatus;
import com.rem.backend.purchasemanagement.enums.PoStatus;
import com.rem.backend.purchasemanagement.repository.*;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.repository.VendorAccountRepo;
import com.rem.backend.enums.ReceiptType;
import com.rem.backend.service.JournalEntryService;
import com.rem.backend.utility.JournalUtilities;
import com.rem.backend.warehousemanagement.entity.Warehouse;
import com.rem.backend.warehousemanagement.repo.WarehouseRepository;
import com.rem.backend.warehousemanagement.service.WarehouseIntegrationService;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final WarehouseIntegrationService warehouseIntegrationService;
    private final WarehouseRepository warehouseRepository;
    private final ProjectRepo projectRepo;
    private final VendorAccountRepo vendorAccountRepo;
    private final ItemsRepo itemsRepo;
    private final JournalEntryService journalEntryService;
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
            // 2️⃣ Validate Receipt Type & Warehouse / Project
            // ===========================
            validateReceiptTypeAndWarehouse(grnInput);

            // ===========================
            // 3️⃣ Fetch and Validate PO
            // ===========================
            PurchaseOrder po = poRepository.findById(grnInput.getPoId())
                    .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found"));

            if (po.getStatus() == PoStatus.CLOSED || po.getStatus() == PoStatus.CANCELLED) {
                throw new IllegalArgumentException("Cannot create GRN for closed or cancelled Purchase Order");
            }

            // ===========================
            // 4️⃣ Validate GRN Quantities against PO
            // ===========================
            List<PurchaseOrderItem> poItems = poItemRepository.findAllByPoId(po.getId());
            Map<Long, PurchaseOrderItem> poItemMap = new HashMap<>();
            for (PurchaseOrderItem item : poItems) {
                poItemMap.put(item.getId(), item);
            }

            for (GrnItems grnItem : grnInput.getGrnItemsList()) {
                ValidationService.validate(grnItem.getPoItemId(), "PO Item Id");
                ValidationService.validate(grnItem.getQuantityReceived(), "Quantity Received");

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
            // 5️⃣ Create GRN
            // ===========================
            Grn grn = new Grn();
            grn.setGrnNumber(generateGRNNumber());
            grn.setOrgId(po.getOrgId());
            grn.setProjectId(po.getProjectId());
            grn.setVendorId(po.getVendorId());
            grn.setPoId(po.getId());
            grn.setStatus(GrnStatus.RECEIVED);
            grn.setReceivedDate(grnInput.getReceivedDate() != null ? grnInput.getReceivedDate() : now);
            grn.setReceiptType(grnInput.getReceiptType());
            grn.setWarehouseId(grnInput.getWarehouseId());
            grn.setDirectProjectId(grnInput.getDirectProjectId());
            grn.setCreatedBy(loggedInUser);
            grn.setUpdatedBy(loggedInUser);
            grn.setCreatedDate(now);
            grn.setUpdatedDate(now);

            grn = grnRepo.save(grn);

            // ===========================
            // 6️⃣ Save GRN Items and Update PO Item Received Quantities
            // ===========================
            Map<Long, Double> itemRateMap = new HashMap<>();
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

                // Collect rate from PO item for warehouse stock
                itemRateMap.put(grnItem.getPoItemId(), poItem.getRate());

                // Update received quantity in PO item
                Double currentReceived = poItem.getReceivedQuantity() != null ? poItem.getReceivedQuantity() : 0.0;
                poItem.setReceivedQuantity(currentReceived + grnItem.getQuantityReceived());
                poItem.setUpdatedBy(loggedInUser);
                poItem.setUpdatedDate(now);
                poItemRepository.save(poItem);
            }

            // ===========================
            // 7️⃣ Update PO Status based on received quantities
            // ===========================
            updatePOStatusAfterGRN(po.getId(), loggedInUser);

            // ===========================
            // 8️⃣ Process warehouse integration based on receipt type
            // ===========================
            if (grn.getReceiptType() == ReceiptType.STOCK && grn.getWarehouseId() != null) {
                List<GrnItems> grnItemsList = grnItemsRepo.findByGrnId(grn.getId());
                warehouseIntegrationService.processGrnApprovalWithRate(grn, grnItemsList, itemRateMap, loggedInUser);
            }
            // If DIRECT_CONSUME — no stock entry needed, goods consumed directly by project


            journalEntryService.createJournalEntryForGrn(
                    grn,
                    poItemMap,
                    grnInput.getGrnItemsList(),
                    loggedInUser
            );

            return ResponseMapper.buildResponse(Responses.SUCCESS, "GRN created successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 2. UPDATE GRN ====================
    @Transactional
    public Map<String, Object> updateGrnOld(Long grnId, Grn grnInput, String loggedInUser) {
        LocalDateTime now = LocalDateTime.now();

        try {
            // ===========================
            // 1️⃣ Basic Validations
            // ===========================
            ValidationService.validate(grnId, "GRN ID");
            ValidationService.validate(grnInput.getPoId(), "Purchase Order Id");

            if (grnInput.getGrnItemsList() == null || grnInput.getGrnItemsList().isEmpty()) {
                throw new IllegalArgumentException("GRN must contain at least one item");
            }

            // ===========================
            // 2️⃣ Validate Receipt Type & Warehouse / Project
            // ===========================
            validateReceiptTypeAndWarehouse(grnInput);

            // ===========================
            // 3️⃣ Fetch and Validate Existing GRN
            // ===========================
            Grn existingGrn = grnRepo.findById(grnId)
                    .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

            if ( existingGrn.getStatus() == GrnStatus.CANCELLED) {
                throw new IllegalArgumentException("Cannot update closed or cancelled GRN");
            }

            // ===========================
            // 4️⃣ Fetch and Validate PO
            // ===========================
            PurchaseOrder po = poRepository.findById(grnInput.getPoId())
                    .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found"));

            if (po.getStatus() == PoStatus.CLOSED || po.getStatus() == PoStatus.CANCELLED) {
                throw new IllegalArgumentException("Cannot update GRN for closed or cancelled Purchase Order");
            }

            // Ensure GRN belongs to the same PO
            if (!existingGrn.getPoId().equals(grnInput.getPoId())) {
                throw new IllegalArgumentException("Cannot change PO ID in GRN update");
            }

            // ===========================
            // 5️⃣ Reverse old warehouse stock if previously set
            // ===========================
            List<GrnItems> existingGrnItems = grnItemsRepo.findByGrnId(grnId);
            if (existingGrn.getReceiptType() == ReceiptType.STOCK && existingGrn.getWarehouseId() != null) {
                warehouseIntegrationService.reverseGrnStock(existingGrn, existingGrnItems, loggedInUser);
            }

            // ===========================
            // 6️⃣ Get PO items and revert PO quantities
            // ===========================
            List<PurchaseOrderItem> poItems = poItemRepository.findAllByPoId(po.getId());
            Map<Long, PurchaseOrderItem> poItemMap = new HashMap<>();
            for (PurchaseOrderItem item : poItems) {
                poItemMap.put(item.getId(), item);
            }

            // Revert previously received quantities
            for (GrnItems existingItem : existingGrnItems) {
                PurchaseOrderItem poItem = poItemMap.get(existingItem.getPoItemId());
                if (poItem != null) {
                    Double currentReceived = poItem.getReceivedQuantity() != null ? poItem.getReceivedQuantity() : 0.0;
                    poItem.setReceivedQuantity(currentReceived - existingItem.getQuantityReceived());
                    poItem.setUpdatedBy(loggedInUser);
                    poItem.setUpdatedDate(now);
                    poItemRepository.save(poItem);
                }
            }

            // ===========================
            // 7️⃣ Validate new GRN quantities against updated PO quantities
            // ===========================
            for (GrnItems newGrnItem : grnInput.getGrnItemsList()) {
                ValidationService.validate(newGrnItem.getPoItemId(), "PO Item Id");
                ValidationService.validate(newGrnItem.getQuantityReceived(), "Quantity Received");

                PurchaseOrderItem poItem = poItemMap.get(newGrnItem.getPoItemId());
                if (poItem == null) {
                    throw new IllegalArgumentException("PO Item not found with id: " + newGrnItem.getPoItemId());
                }

                // Calculate total already received for this PO item (excluding current GRN)
                Double alreadyReceivedFromOtherGRNs = grnItemsRepo.getTotalReceivedQuantityByPoItemId(newGrnItem.getPoItemId());
                Double alreadyReceivedFromCurrentGRN = existingGrnItems.stream()
                        .filter(item -> item.getPoItemId().equals(newGrnItem.getPoItemId()))
                        .mapToDouble(GrnItems::getQuantityReceived)
                        .sum();

                Double netAlreadyReceived = alreadyReceivedFromOtherGRNs - alreadyReceivedFromCurrentGRN;
                Double pendingQuantity = poItem.getQuantity() - netAlreadyReceived;

                if (newGrnItem.getQuantityReceived() > pendingQuantity) {
                    throw new IllegalArgumentException("GRN quantity (" + newGrnItem.getQuantityReceived() +
                            ") exceeds pending quantity (" + pendingQuantity + ") for PO Item: " + newGrnItem.getPoItemId());
                }
            }

            // ===========================
            // 8️⃣ Delete existing GRN items
            // ===========================
            grnItemsRepo.deleteAll(existingGrnItems);

            // ===========================
            // 9️⃣ Update GRN header
            // ===========================
            existingGrn.setReceivedDate(grnInput.getReceivedDate() != null ? grnInput.getReceivedDate() : existingGrn.getReceivedDate());
            existingGrn.setReceiptType(grnInput.getReceiptType());
            existingGrn.setWarehouseId(grnInput.getWarehouseId());
            existingGrn.setDirectProjectId(grnInput.getDirectProjectId());
            existingGrn.setUpdatedBy(loggedInUser);
            existingGrn.setUpdatedDate(now);

            grnRepo.save(existingGrn);

            // ===========================
            // 🔟 Save new GRN Items and Update PO Item Received Quantities
            // ===========================
            Map<Long, Double> itemRateMap = new HashMap<>();
            for (GrnItems grnItem : grnInput.getGrnItemsList()) {
                PurchaseOrderItem poItem = poItemMap.get(grnItem.getPoItemId());

                GrnItems newGrnItem = new GrnItems();
                newGrnItem.setGrnId(existingGrn.getId());
                newGrnItem.setPoItemId(grnItem.getPoItemId());
                newGrnItem.setItemId(poItem.getItems().getId());
                newGrnItem.setQuantityReceived(grnItem.getQuantityReceived());
                newGrnItem.setQuantityInvoiced(0.0);
                newGrnItem.setCreatedBy(loggedInUser);
                newGrnItem.setUpdatedBy(loggedInUser);
                newGrnItem.setCreatedDate(now);
                newGrnItem.setUpdatedDate(now);

                grnItemsRepo.save(newGrnItem);

                // Collect rate from PO item for warehouse stock
                itemRateMap.put(grnItem.getPoItemId(), poItem.getRate());

                // Update received quantity in PO item with new quantities
                Double currentReceived = poItem.getReceivedQuantity() != null ? poItem.getReceivedQuantity() : 0.0;
                poItem.setReceivedQuantity(currentReceived + grnItem.getQuantityReceived());
                poItem.setUpdatedBy(loggedInUser);
                poItem.setUpdatedDate(now);
                poItemRepository.save(poItem);
            }

            // ===========================
            // 1️⃣1️⃣ Update PO Status based on received quantities
            // ===========================
            updatePOStatusAfterGRN(po.getId(), loggedInUser);

            // ===========================
            // 1️⃣2️⃣ Process new warehouse integration based on receipt type
            // ===========================
            if (existingGrn.getReceiptType() == ReceiptType.STOCK && existingGrn.getWarehouseId() != null) {
                List<GrnItems> updatedGrnItemsList = grnItemsRepo.findByGrnId(existingGrn.getId());
                warehouseIntegrationService.processGrnApprovalWithRate(existingGrn, updatedGrnItemsList, itemRateMap, loggedInUser);
            }
            // If DIRECT_CONSUME — no stock entry needed

            return ResponseMapper.buildResponse(Responses.SUCCESS, "GRN updated successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 2. UPDATE GRN ====================
    @Transactional
    public Map<String, Object> updateGrn(Long grnId, Grn grnInput, String loggedInUser) {

        LocalDateTime now = LocalDateTime.now();

        try {
            // ===========================
            // 1️⃣ Basic Validations
            // ===========================
            ValidationService.validate(grnId, "GRN ID");
            ValidationService.validate(grnInput.getPoId(), "Purchase Order Id");

            if (grnInput.getGrnItemsList() == null || grnInput.getGrnItemsList().isEmpty()) {
                throw new IllegalArgumentException("GRN must contain at least one item");
            }

            // ===========================
            // 2️⃣ Validate Receipt Type & Warehouse / Project
            // ===========================
            validateReceiptTypeAndWarehouse(grnInput);

            // ===========================
            // 3️⃣ Fetch and Validate Existing GRN
            // ===========================
            Grn existingGrn = grnRepo.findById(grnId)
                    .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

            if (existingGrn.getStatus() == GrnStatus.CANCELLED) {
                throw new IllegalArgumentException("Cannot update cancelled GRN");
            }

            // ✅ IMPORTANT FOR ACCOUNTING
            // Store old receipt type before updating header
            ReceiptType oldReceiptType = existingGrn.getReceiptType();

            // ===========================
            // 4️⃣ Fetch and Validate PO
            // ===========================
            PurchaseOrder po = poRepository.findById(grnInput.getPoId())
                    .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found"));

            if (po.getStatus() == PoStatus.CLOSED || po.getStatus() == PoStatus.CANCELLED) {
                throw new IllegalArgumentException("Cannot update GRN for closed or cancelled Purchase Order");
            }

            // Ensure GRN belongs to the same PO
            if (!existingGrn.getPoId().equals(grnInput.getPoId())) {
                throw new IllegalArgumentException("Cannot change PO ID in GRN update");
            }

            // ===========================
            // 5️⃣ Fetch Existing GRN Items
            // ===========================
            List<GrnItems> existingGrnItems = grnItemsRepo.findByGrnId(grnId);

            // ===========================
            // 6️⃣ Reverse Old Warehouse Stock If Previously STOCK
            // ===========================
            if (existingGrn.getReceiptType() == ReceiptType.STOCK
                    && existingGrn.getWarehouseId() != null) {

                warehouseIntegrationService.reverseGrnStock(
                        existingGrn,
                        existingGrnItems,
                        loggedInUser
                );
            }

            // ===========================
            // 7️⃣ Get PO Items
            // ===========================
            List<PurchaseOrderItem> poItems = poItemRepository.findAllByPoId(po.getId());

            Map<Long, PurchaseOrderItem> poItemMap = new HashMap<>();
            for (PurchaseOrderItem item : poItems) {
                poItemMap.put(item.getId(), item);
            }

            // ===========================
            // 8️⃣ Revert Previously Received Quantities From PO Items
            // ===========================
            for (GrnItems existingItem : existingGrnItems) {

                PurchaseOrderItem poItem = poItemMap.get(existingItem.getPoItemId());

                if (poItem != null) {
                    Double currentReceived = poItem.getReceivedQuantity() != null
                            ? poItem.getReceivedQuantity()
                            : 0.0;

                    poItem.setReceivedQuantity(currentReceived - existingItem.getQuantityReceived());
                    poItem.setUpdatedBy(loggedInUser);
                    poItem.setUpdatedDate(now);

                    poItemRepository.save(poItem);
                }
            }

            // ===========================
            // 9️⃣ Validate New GRN Quantities Against PO
            // ===========================
            for (GrnItems newGrnItem : grnInput.getGrnItemsList()) {

                ValidationService.validate(newGrnItem.getPoItemId(), "PO Item Id");
                ValidationService.validate(newGrnItem.getQuantityReceived(), "Quantity Received");

                PurchaseOrderItem poItem = poItemMap.get(newGrnItem.getPoItemId());

                if (poItem == null) {
                    throw new IllegalArgumentException(
                            "PO Item not found with id: " + newGrnItem.getPoItemId()
                    );
                }

                Double alreadyReceivedFromOtherGRNs =
                        grnItemsRepo.getTotalReceivedQuantityByPoItemId(newGrnItem.getPoItemId());

                alreadyReceivedFromOtherGRNs =
                        alreadyReceivedFromOtherGRNs != null ? alreadyReceivedFromOtherGRNs : 0.0;

                Double alreadyReceivedFromCurrentGRN = existingGrnItems.stream()
                        .filter(item -> item.getPoItemId().equals(newGrnItem.getPoItemId()))
                        .mapToDouble(GrnItems::getQuantityReceived)
                        .sum();

                Double netAlreadyReceived =
                        alreadyReceivedFromOtherGRNs - alreadyReceivedFromCurrentGRN;

                Double pendingQuantity =
                        poItem.getQuantity() - netAlreadyReceived;

                if (newGrnItem.getQuantityReceived() > pendingQuantity) {
                    throw new IllegalArgumentException(
                            "GRN quantity (" + newGrnItem.getQuantityReceived() +
                                    ") exceeds pending quantity (" + pendingQuantity +
                                    ") for PO Item: " + newGrnItem.getPoItemId()
                    );
                }
            }

            // ===========================
            // 🔟 Delete Existing GRN Items
            // ===========================
            grnItemsRepo.deleteAll(existingGrnItems);

            // ===========================
            // 1️⃣1️⃣ Update GRN Header
            // ===========================
            existingGrn.setReceivedDate(
                    grnInput.getReceivedDate() != null
                            ? grnInput.getReceivedDate()
                            : existingGrn.getReceivedDate()
            );

            existingGrn.setReceiptType(grnInput.getReceiptType());
            existingGrn.setWarehouseId(grnInput.getWarehouseId());
            existingGrn.setDirectProjectId(grnInput.getDirectProjectId());
            existingGrn.setUpdatedBy(loggedInUser);
            existingGrn.setUpdatedDate(now);

            existingGrn = grnRepo.save(existingGrn);

            // ===========================
            // 1️⃣2️⃣ Save New GRN Items and Update PO Received Quantities
            // ===========================
            Map<Long, Double> itemRateMap = new HashMap<>();

            for (GrnItems grnItem : grnInput.getGrnItemsList()) {

                PurchaseOrderItem poItem = poItemMap.get(grnItem.getPoItemId());

                GrnItems newGrnItem = new GrnItems();
                newGrnItem.setGrnId(existingGrn.getId());
                newGrnItem.setPoItemId(grnItem.getPoItemId());
                newGrnItem.setItemId(poItem.getItems().getId());
                newGrnItem.setQuantityReceived(grnItem.getQuantityReceived());
                newGrnItem.setQuantityInvoiced(0.0);
                newGrnItem.setCreatedBy(loggedInUser);
                newGrnItem.setUpdatedBy(loggedInUser);
                newGrnItem.setCreatedDate(now);
                newGrnItem.setUpdatedDate(now);

                grnItemsRepo.save(newGrnItem);

                // Collect rate from PO item for warehouse/accounting
                itemRateMap.put(grnItem.getPoItemId(), poItem.getRate());

                // Update received quantity in PO item with new GRN quantity
                Double currentReceived = poItem.getReceivedQuantity() != null
                        ? poItem.getReceivedQuantity()
                        : 0.0;

                poItem.setReceivedQuantity(currentReceived + grnItem.getQuantityReceived());
                poItem.setUpdatedBy(loggedInUser);
                poItem.setUpdatedDate(now);

                poItemRepository.save(poItem);
            }

            // ===========================
            // 1️⃣3️⃣ Update PO Status Based On Received Quantities
            // ===========================
            updatePOStatusAfterGRN(po.getId(), loggedInUser);

            // ===========================
            // 1️⃣4️⃣ Fetch Updated GRN Items
            // ===========================
            List<GrnItems> updatedGrnItemsList =
                    grnItemsRepo.findByGrnId(existingGrn.getId());

            // ===========================
            // 1️⃣5️⃣ Process New Warehouse Integration
            // ===========================
            if (existingGrn.getReceiptType() == ReceiptType.STOCK
                    && existingGrn.getWarehouseId() != null) {

                warehouseIntegrationService.processGrnApprovalWithRate(
                        existingGrn,
                        updatedGrnItemsList,
                        itemRateMap,
                        loggedInUser
                );
            }

            // If DIRECT_CONSUME — no warehouse stock entry needed

            // ===========================
            // 1️⃣6️⃣ Create Accounting Journal Entry For GRN Update
            // ===========================
            journalEntryService.updateJournalEntryForGrn(
                    existingGrn,
                    oldReceiptType,
                    existingGrnItems,
                    updatedGrnItemsList,
                    poItemMap,
                    loggedInUser
            );

            return ResponseMapper.buildResponse(
                    Responses.SUCCESS,
                    "GRN updated successfully"
            );

        } catch (IllegalArgumentException e) {

            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

            return ResponseMapper.buildResponse(
                    Responses.INVALID_PARAMETER,
                    e.getMessage()
            );

        } catch (Exception e) {

            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

            return ResponseMapper.buildResponse(
                    Responses.SYSTEM_FAILURE,
                    e.getMessage()
            );
        }
    }


    // ==================== 3. GET GRN BY ID ====================
    @Transactional
    public Map<String, Object> getById(Long grnId) {
        try {
            Grn grn = grnRepo.findById(grnId)
                    .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

            List<GrnItems> items = grnItemsRepo.findByGrnId(grnId);

            // Populate itemName for each GRN item
            items.forEach(grnItem -> {
                if (grnItem.getItemId() != null) {
                    itemsRepo.findById(grnItem.getItemId()).ifPresent(item -> grnItem.setItemName(item.getName()));
                }
            });

            grn.setGrnItemsList(items);

            // Populate projectName
            if (grn.getProjectId() != null) {
                projectRepo.findById(grn.getProjectId())
                        .ifPresent(project -> grn.setProjectName(project.getName()));
            }

            // Populate vendorName
            if (grn.getVendorId() != null) {
                vendorAccountRepo.findById(grn.getVendorId())
                        .ifPresent(vendor -> grn.setVendorName(vendor.getName()));
            }

            // Populate poNumber
            if (grn.getPoId() != null) {
                poRepository.findById(grn.getPoId())
                        .ifPresent(po -> grn.setPoNumber(po.getPoNumber()));
            }

            // Populate warehouseName
            if (grn.getWarehouseId() != null) {
                warehouseRepository.findById(grn.getWarehouseId())
                        .ifPresent(wh -> grn.setWarehouseName(wh.getName()));
            }

            // Populate directProjectName
            if (grn.getDirectProjectId() != null) {
                projectRepo.findById(grn.getDirectProjectId())
                        .ifPresent(project -> grn.setDirectProjectName(project.getName()));
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, grn);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 4. GET ALL GRNs BY PO ID ====================
    @Transactional
    public Map<String, Object> getByPoId(Long poId, Pageable pageable) {
        try {
            // Use pageable repo method
            Page<Grn> grnPage = grnRepo.findByPoId(poId, pageable);

            List<Grn> grnList = grnPage.getContent();

            // Populate items and transient display fields
            for (Grn grn : grnList) {
                List<GrnItems> items = grnItemsRepo.findByGrnId(grn.getId());
                // Populate itemName for each GRN item
                items.forEach(grnItem -> {
                    if (grnItem.getItemId() != null) {
                        itemsRepo.findById(grnItem.getItemId()).ifPresent(item -> grnItem.setItemName(item.getName()));
                    }
                });
                grn.setGrnItemsList(items);

                // Populate poNumber and poStatus
                if (grn.getPoId() != null) {
                    poRepository.findById(grn.getPoId()).ifPresent(po -> {
                        grn.setPoNumber(po.getPoNumber());
                        grn.setPoStatus(po.getStatus());
                    });
                }

                // Populate projectName
                if (grn.getProjectId() != null) {
                    projectRepo.findById(grn.getProjectId())
                            .ifPresent(project -> grn.setProjectName(project.getName()));
                }

                // Populate vendorName
                if (grn.getVendorId() != null) {
                    vendorAccountRepo.findById(grn.getVendorId())
                            .ifPresent(vendor -> grn.setVendorName(vendor.getName()));
                }

                // Populate warehouseName
                if (grn.getWarehouseId() != null) {
                    warehouseRepository.findById(grn.getWarehouseId())
                            .ifPresent(wh -> grn.setWarehouseName(wh.getName()));
                }

                // Populate directProjectName
                if (grn.getDirectProjectId() != null) {
                    projectRepo.findById(grn.getDirectProjectId())
                            .ifPresent(project -> grn.setDirectProjectName(project.getName()));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("content", grnList);
            response.put("page", grnPage.getNumber());
            response.put("size", grnPage.getSize());
            response.put("totalElements", grnPage.getTotalElements());
            response.put("totalPages", grnPage.getTotalPages());
            response.put("last", grnPage.isLast());

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);
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

    // ==================== GET GRNs GROUPED BY PO ID ====================
    public Map<String, Object> getGrnGroupedByPoId(Long orgId, Pageable pageable) {
        try {
            ValidationService.validate(orgId, "Organization ID");

            Page<Object[]> grnGroupedPage = grnRepo.findGrnGroupedByPoId(orgId, pageable);

            List<GrnByPoGroupedResponseDTO> responseList = grnGroupedPage.getContent().stream()
                    .map(this::mapToGrnByPoGroupedResponseDTO)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("content", responseList);
            response.put("totalElements", grnGroupedPage.getTotalElements());
            response.put("totalPages", grnGroupedPage.getTotalPages());
            response.put("currentPage", grnGroupedPage.getNumber());
            response.put("pageSize", grnGroupedPage.getSize());
            response.put("hasNext", grnGroupedPage.hasNext());
            response.put("hasPrevious", grnGroupedPage.hasPrevious());

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    private GrnByPoGroupedResponseDTO mapToGrnByPoGroupedResponseDTO(Object[] row) {
        GrnByPoGroupedResponseDTO dto = new GrnByPoGroupedResponseDTO();
        dto.setPoId(((Number) row[0]).longValue());
        dto.setPoNumber((String) row[1]);
        dto.setVendorId(row[2] != null ? ((Number) row[2]).longValue() : null);
        dto.setVendorName((String) row[3]);
        dto.setProjectId(row[4] != null ? ((Number) row[4]).longValue() : null);
        dto.setProjectName((String) row[5]);
        dto.setPoDate(row[6] != null ? ((java.sql.Timestamp) row[6]).toLocalDateTime() : null);
        dto.setTotalGrnCount(((Number) row[7]).longValue());
        dto.setLastGrnDate(row[8] != null ? ((java.sql.Timestamp) row[8]).toLocalDateTime() : null);
        dto.setFirstGrnDate(row[9] != null ? ((java.sql.Timestamp) row[9]).toLocalDateTime() : null);
        return dto;
    }

    // ==================== GET GRNs BY CONDITIONAL FILTERS (orgId REQUIRED, others optional) ====================
    public Map<String, Object> getByConditionalFilters(
            Long orgId,
            Long poId,
            Long vendorId,
            GrnStatus status,
            LocalDate startDate,
            LocalDate endDate,
            GrnInvoiceStatus invoiceStatus,
            Long warehouseId,
            ReceiptType receiptType,
            Pageable pageable) {
        try {
            // Validate mandatory orgId
            ValidationService.validate(orgId, "Organization ID");

            // Call repository with all parameters
            Page<Grn> grnPage = grnRepo.findByConditionalFilters(
                    orgId,
                    poId,
                    vendorId,
                    status,
                    startDate,
                    endDate,
                    invoiceStatus,
                    warehouseId,
                    receiptType,
                    pageable
            );

            // Fetch GRN items for each GRN and populate transient fields
            List<Grn> grnsWithItems = grnPage.getContent().stream()
                    .peek(grn -> {
                        List<GrnItems> items = grnItemsRepo.findByGrnId(grn.getId());

                        // Populate itemName for each GRN item
                        items.forEach(grnItem -> {
                            if (grnItem.getItemId() != null) {
                                itemsRepo.findById(grnItem.getItemId())
                                        .ifPresent(item -> grnItem.setItemName(item.getName()));
                            }
                        });

                        grn.setGrnItemsList(items);

                        // Populate projectName
                        if (grn.getProjectId() != null) {
                            projectRepo.findById(grn.getProjectId())
                                    .ifPresent(project -> grn.setProjectName(project.getName()));
                        }

                        // Populate vendorName
                        if (grn.getVendorId() != null) {
                            vendorAccountRepo.findById(grn.getVendorId())
                                    .ifPresent(vendor -> grn.setVendorName(vendor.getName()));
                        }

                        // Populate poNumber
                        if (grn.getPoId() != null) {
                            poRepository.findById(grn.getPoId())
                                    .ifPresent(po -> grn.setPoNumber(po.getPoNumber()));
                        }

                        // Populate warehouseName
                        if (grn.getWarehouseId() != null) {
                            warehouseRepository.findById(grn.getWarehouseId())
                                    .ifPresent(wh -> grn.setWarehouseName(wh.getName()));
                        }

                        // Populate directProjectName
                        if (grn.getDirectProjectId() != null) {
                            projectRepo.findById(grn.getDirectProjectId())
                                    .ifPresent(project -> grn.setDirectProjectName(project.getName()));
                        }
                    })
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("content", grnsWithItems);
            response.put("totalElements", grnPage.getTotalElements());
            response.put("totalPages", grnPage.getTotalPages());
            response.put("currentPage", grnPage.getNumber());
            response.put("pageSize", grnPage.getSize());
            response.put("hasNext", grnPage.hasNext());
            response.put("hasPrevious", grnPage.hasPrevious());

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== HELPER: Validate Receipt Type and Warehouse ====================
    private void validateReceiptTypeAndWarehouse(Grn grnInput) {
        if (grnInput.getReceiptType() != null) {
            if (grnInput.getReceiptType() == ReceiptType.STOCK) {
                if (grnInput.getWarehouseId() == null) {
                    throw new IllegalArgumentException("Warehouse ID is required when receipt type is STOCK");
                }
                // Validate warehouse exists and is active
                Warehouse warehouse = warehouseRepository.findById(grnInput.getWarehouseId())
                        .orElseThrow(() -> new IllegalArgumentException("Warehouse not found with ID: " + grnInput.getWarehouseId()));
                if (!warehouse.getActive()) {
                    throw new IllegalArgumentException("Cannot use inactive warehouse");
                }
                // Clear directProjectId if receipt type is STOCK
                grnInput.setDirectProjectId(null);

            } else if (grnInput.getReceiptType() == ReceiptType.DIRECT) {
                if (grnInput.getDirectProjectId() == null) {
                    throw new IllegalArgumentException("Direct project ID is required when receipt type is DIRECT");
                }
                // Validate project exists
                projectRepo.findById(grnInput.getDirectProjectId())
                        .orElseThrow(() -> new IllegalArgumentException("Project not found with ID: " + grnInput.getDirectProjectId()));
                // Clear warehouseId if receipt type is DIRECT
                grnInput.setWarehouseId(null);
            }
        }
    }

    // ==================== CANCEL GRN ====================
    @Transactional
    public Map<String, Object> cancelGrn(Long grnId, String loggedInUser) {

        LocalDateTime now = LocalDateTime.now();

        try {
            // ===========================
            // 1️⃣ Basic Validation
            // ===========================
            ValidationService.validate(grnId, "GRN ID");

            // ===========================
            // 2️⃣ Fetch and Validate GRN
            // ===========================
            Grn grn = grnRepo.findById(grnId)
                    .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

            if (grn.getStatus() == GrnStatus.CANCELLED) {
                throw new IllegalArgumentException("GRN is already cancelled");
            }

            // ===========================
            // 3️⃣ Validate Invoice Status
            // ===========================
            if (grn.getInvoiceStatus() != null
                    && grn.getInvoiceStatus() != GrnInvoiceStatus.NOT_INVOICED) {

                throw new IllegalArgumentException(
                        "Cannot cancel GRN that has invoiced items. Please cancel invoices first."
                );
            }

            // ===========================
            // 4️⃣ Fetch GRN Items
            // ===========================
            List<GrnItems> grnItems = grnItemsRepo.findByGrnId(grnId);

            if (grnItems == null || grnItems.isEmpty()) {
                throw new IllegalArgumentException("No GRN items found for cancellation");
            }

            // ===========================
            // 5️⃣ Fetch PO Items For Accounting + Quantity Reversal
            // ===========================
            List<PurchaseOrderItem> poItems = poItemRepository.findAllByPoId(grn.getPoId());

            Map<Long, PurchaseOrderItem> poItemMap = new HashMap<>();
            for (PurchaseOrderItem item : poItems) {
                poItemMap.put(item.getId(), item);
            }

            // ===========================
            // 6️⃣ Reverse Warehouse Stock If STOCK GRN
            // ===========================
            if (grn.getReceiptType() == ReceiptType.STOCK
                    && grn.getWarehouseId() != null) {

                warehouseIntegrationService.reverseGrnStock(
                        grn,
                        grnItems,
                        loggedInUser
                );
            }

            // ===========================
            // 7️⃣ Revert PO Received Quantities
            // ===========================
            for (GrnItems grnItem : grnItems) {

                if (grnItem.getPoItemId() != null) {

                    PurchaseOrderItem poItem = poItemMap.get(grnItem.getPoItemId());

                    if (poItem != null) {
                        Double currentReceived = poItem.getReceivedQuantity() != null
                                ? poItem.getReceivedQuantity()
                                : 0.0;

                        poItem.setReceivedQuantity(
                                Math.max(0, currentReceived - grnItem.getQuantityReceived())
                        );

                        poItem.setUpdatedBy(loggedInUser);
                        poItem.setUpdatedDate(now);

                        poItemRepository.save(poItem);
                    }
                }
            }

            // ===========================
            // 8️⃣ Create Accounting Journal Entry For GRN Cancel
            // ===========================
            journalEntryService.cancelJournalEntryForGrn(
                    grn,
                    poItemMap,
                    grnItems,
                    loggedInUser
            );

            // ===========================
            // 9️⃣ Update GRN Status To CANCELLED
            // ===========================
            grn.setStatus(GrnStatus.CANCELLED);
            grn.setUpdatedBy(loggedInUser);
            grn.setUpdatedDate(now);

            grnRepo.save(grn);

            // ===========================
            // 🔟 Recalculate PO Status
            // ===========================
            updatePOStatusAfterGRN(grn.getPoId(), loggedInUser);

            return ResponseMapper.buildResponse(
                    Responses.SUCCESS,
                    "GRN cancelled successfully"
            );

        } catch (IllegalArgumentException e) {

            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

            return ResponseMapper.buildResponse(
                    Responses.INVALID_PARAMETER,
                    e.getMessage()
            );

        } catch (Exception e) {

            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

            return ResponseMapper.buildResponse(
                    Responses.SYSTEM_FAILURE,
                    e.getMessage()
            );
        }
    }


    // ==================== HELPER: Calculate and Update GRN Invoice Status ====================
    /**
     * Calculates GRN invoice status based on all GRN items' invoiced quantities
     * This method should be called after any invoice create, update, or delete operation
     *
     * Business Logic:
     * - If all items have 0 invoiced qty → NOT_INVOICED
     * - If all items are fully invoiced → FULLY_INVOICED
     * - Otherwise → PARTIALLY_INVOICED
     *
     * @param grnId The GRN ID to calculate status for
     * @param loggedInUser Username for audit trail
     * @throws IllegalArgumentException if over-invoicing is detected
     */
    @Transactional
    public void calculateAndUpdateGrnInvoiceStatus(Long grnId, String loggedInUser) {
        Grn grn = grnRepo.findById(grnId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found with id: " + grnId));

            List<GrnItems> grnItems = grnItemsRepo.findByGrnId(grnId);

            if (grnItems.isEmpty()) {
                throw new IllegalArgumentException("GRN has no items");
            }

            int notInvoicedCount = 0;
            int fullyInvoicedCount = 0;
            int partiallyInvoicedCount = 0;

            for (GrnItems item : grnItems) {
                Double quantityReceived = item.getQuantityReceived() != null ? item.getQuantityReceived() : 0.0;
                Double quantityInvoiced = item.getQuantityInvoiced() != null ? item.getQuantityInvoiced() : 0.0;

                // Validation: Prevent over-invoicing
                if (quantityInvoiced > quantityReceived) {
                    throw new IllegalArgumentException(
                        "Over-invoicing detected for GRN Item ID: " + item.getId() +
                        ". Invoiced quantity (" + quantityInvoiced +
                        ") exceeds received quantity (" + quantityReceived + ")"
                    );
                }

                // Categorize item status
                if (quantityInvoiced == 0) {
                    notInvoicedCount++;
                } else if (quantityInvoiced.equals(quantityReceived)) {
                    fullyInvoicedCount++;
                } else {
                    partiallyInvoicedCount++;
                }
            }

            // Determine GRN level status
            GrnInvoiceStatus newStatus;

            if (notInvoicedCount == grnItems.size()) {
                // All items not invoiced
                newStatus = GrnInvoiceStatus.NOT_INVOICED;
            } else if (fullyInvoicedCount == grnItems.size()) {
                // All items fully invoiced
                newStatus = GrnInvoiceStatus.FULLY_INVOICED;
            } else {
                // Mixed state - at least one item is partially invoiced or some items invoiced, some not
                newStatus = GrnInvoiceStatus.PARTIALLY_INVOICED;
            }

            // Update GRN status if changed
            if (grn.getInvoiceStatus() != newStatus) {
                grn.setInvoiceStatus(newStatus);
                grn.setUpdatedBy(loggedInUser);
                grn.setUpdatedDate(LocalDateTime.now());
                grnRepo.save(grn);
            }
    }
}
