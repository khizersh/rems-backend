package com.rem.backend.purchasemanagement.service;

import com.rem.backend.purchasemanagement.entity.grn.Grn;
import com.rem.backend.purchasemanagement.entity.grn.GrnItems;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrder;
import com.rem.backend.purchasemanagement.entity.vendorinvoice.VendorInvoice;
import com.rem.backend.purchasemanagement.entity.vendorinvoice.VendorInvoiceItem;
import com.rem.backend.purchasemanagement.enums.InvoiceStatus;
import com.rem.backend.purchasemanagement.repository.*;
import com.rem.backend.projectmanagement.repository.ProjectRepo;
import com.rem.backend.vendormanagement.repository.VendorAccountRepo;
import com.rem.backend.accountingmanagement.service.JournalEntryService;
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
public class VendorInvoiceService {

    private final VendorInvoiceRepo vendorInvoiceRepo;
    private final VendorInvoiceItemRepo vendorInvoiceItemRepo;
    private final GrnRepo grnRepo;
    private final GrnItemsRepo grnItemsRepo;
    private final PurchaseOrderRepo poRepository;
    private final ProjectRepo projectRepo;
    private final VendorAccountRepo vendorAccountRepo;
    private final ItemsRepo itemsRepo;
    private final GrnService grnService;
    private final JournalEntryService journalEntryService;

    // ==================== 1. CREATE INVOICE AGAINST GRN ====================
    @Transactional
    public Map<String, Object> createInvoice(VendorInvoice invoiceInput, String loggedInUser) {

        LocalDateTime now = LocalDateTime.now();

        try {
            // ===========================
            // 1️⃣ Basic Invoice Validations
            // ===========================
            ValidationService.validate(invoiceInput.getGrnId(), "GRN Id");
            ValidationService.validate(invoiceInput.getTotalAmount(), "Total Amount");

            if (invoiceInput.getInvoiceItemList() == null || invoiceInput.getInvoiceItemList().isEmpty()) {
                throw new IllegalArgumentException("Invoice must contain at least one item");
            }

            // ===========================
            // 2️⃣ Fetch and Validate GRN
            // ===========================
            Grn grn = grnRepo.findById(invoiceInput.getGrnId())
                    .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

            PurchaseOrder po = poRepository.findById(grn.getPoId())
                    .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found"));

            // ===========================
            // 3️⃣ Validate Invoice Quantities Against GRN (Prevent invoice qty > GRN qty)
            // ===========================
            List<GrnItems> grnItems = grnItemsRepo.findByGrnId(grn.getId());
            Map<Long, GrnItems> grnItemMap = new HashMap<>();
            for (GrnItems item : grnItems) {
                grnItemMap.put(item.getId(), item);
            }

            double calculatedTotal = 0;

            for (VendorInvoiceItem invoiceItem : invoiceInput.getInvoiceItemList()) {
                ValidationService.validate(invoiceItem.getGrnItemId(), "GRN Item Id");
                ValidationService.validate(invoiceItem.getQuantity(), "Quantity");
                ValidationService.validate(invoiceItem.getRate(), "Rate");

                if (invoiceItem.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Invoice quantity must be greater than 0");
                }

                GrnItems grnItem = grnItemMap.get(invoiceItem.getGrnItemId());
                if (grnItem == null) {
                    throw new IllegalArgumentException("GRN Item not found with id: " + invoiceItem.getGrnItemId());
                }

                // Calculate already invoiced quantity for this GRN item
                Double alreadyInvoiced = vendorInvoiceItemRepo.getTotalInvoicedQuantityByGrnItemId(invoiceItem.getGrnItemId());
                Double pendingInvoiceQty = grnItem.getQuantityReceived() - alreadyInvoiced;

                if (invoiceItem.getQuantity() > pendingInvoiceQty) {
                    throw new IllegalArgumentException("Invoice quantity (" + invoiceItem.getQuantity() +
                            ") exceeds pending quantity (" + pendingInvoiceQty + ") for GRN Item: " + invoiceItem.getGrnItemId());
                }

                calculatedTotal += invoiceItem.getQuantity() * invoiceItem.getRate();
            }

            // ===========================
            // 4️⃣ Create Invoice
            // ===========================
            VendorInvoice invoice = new VendorInvoice();
            invoice.setInvoiceNumber(invoiceInput.getInvoiceNumber() != null ? invoiceInput.getInvoiceNumber() : generateInvoiceNumber());
            invoice.setOrgId(grn.getOrgId());
            invoice.setProjectId(grn.getProjectId());
            invoice.setVendorId(grn.getVendorId());
            invoice.setPoId(grn.getPoId());
            invoice.setGrnId(grn.getId());
            invoice.setTotalAmount(invoiceInput.getTotalAmount());
            invoice.setPaidAmount(0.0);
            invoice.setPendingAmount(invoiceInput.getTotalAmount()); // Auto-calculate pending amount
            invoice.setStatus(InvoiceStatus.UNPAID);
            invoice.setInvoiceDate(invoiceInput.getInvoiceDate() != null ? invoiceInput.getInvoiceDate() : LocalDate.now());
            invoice.setDueDate(invoiceInput.getDueDate());
            invoice.setCreatedBy(loggedInUser);
            invoice.setUpdatedBy(loggedInUser);
            invoice.setCreatedDate(now);
            invoice.setUpdatedDate(now);

            invoice = vendorInvoiceRepo.save(invoice);

            // ===========================
            // 5️⃣ Save Invoice Items and Update GRN Item Invoiced Quantities
            // ===========================
            for (VendorInvoiceItem invoiceItem : invoiceInput.getInvoiceItemList()) {
                GrnItems grnItem = grnItemMap.get(invoiceItem.getGrnItemId());

                VendorInvoiceItem newInvoiceItem = new VendorInvoiceItem();
                newInvoiceItem.setInvoiceId(invoice.getId());
                newInvoiceItem.setGrnItemId(invoiceItem.getGrnItemId());
                newInvoiceItem.setQuantity(invoiceItem.getQuantity());
                newInvoiceItem.setRate(invoiceItem.getRate());
                newInvoiceItem.setAmount(invoiceItem.getQuantity() * invoiceItem.getRate());
                newInvoiceItem.setCreatedBy(loggedInUser);
                newInvoiceItem.setUpdatedBy(loggedInUser);
                newInvoiceItem.setCreatedDate(now);
                newInvoiceItem.setUpdatedDate(now);

                vendorInvoiceItemRepo.save(newInvoiceItem);

                // Update invoiced quantity in GRN item
                Double currentInvoiced = grnItem.getQuantityInvoiced() != null ? grnItem.getQuantityInvoiced() : 0.0;
                grnItem.setQuantityInvoiced(currentInvoiced + invoiceItem.getQuantity());
                grnItem.setUpdatedBy(loggedInUser);
                grnItem.setUpdatedDate(now);
                grnItemsRepo.save(grnItem);
            }


            // ===========================
// 6️⃣ Fetch Saved Invoice Items For Accounting
// ===========================
            List<VendorInvoiceItem> savedInvoiceItems =
                    vendorInvoiceItemRepo.findByInvoiceId(invoice.getId());

// ===========================
// 7️⃣ Create Accounting Journal Entry
// ===========================
            journalEntryService.createJournalEntryForVendorInvoice(
                    invoice,
                    savedInvoiceItems,
                    loggedInUser
            );

// ===========================
// 8️⃣ Calculate and update GRN invoice status based on all items
// ===========================
            grnService.calculateAndUpdateGrnInvoiceStatus(grn.getId(), loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Invoice created successfully");


        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 2. GET INVOICE BY ID ====================
    @Transactional
    public Map<String, Object> getById(Long invoiceId) {
        try {
            VendorInvoice invoice = vendorInvoiceRepo.findById(invoiceId)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

            populateInvoiceTransientFields(invoice);

            return ResponseMapper.buildResponse(Responses.SUCCESS, invoice);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 3. GET ALL INVOICES BY ORGANIZATION ====================
    @Transactional
    public Map<String, Object> getAllByOrgId(Long orgId, Pageable pageable) {
        try {
            Page<VendorInvoice> invoices = vendorInvoiceRepo.findByOrgId(orgId, pageable);

            for (VendorInvoice invoice : invoices.getContent()) {
                populateInvoiceTransientFields(invoice);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, invoices);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 4. GET INVOICES BY VENDOR ====================
    @Transactional
    public Map<String, Object> getByVendorId(Long vendorId, Pageable pageable) {
        try {
            Page<VendorInvoice> invoices = vendorInvoiceRepo.findByVendorId(vendorId, pageable);

            for (VendorInvoice invoice : invoices.getContent()) {
                populateInvoiceTransientFields(invoice);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, invoices);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 5. GET INVOICES BY STATUS ====================
    @Transactional
    public Map<String, Object> getByStatus(Long orgId, InvoiceStatus status, Pageable pageable) {
        try {
            Page<VendorInvoice> invoices = vendorInvoiceRepo.findByOrgIdAndStatus(orgId, status, pageable);

            for (VendorInvoice invoice : invoices.getContent()) {
                populateInvoiceTransientFields(invoice);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, invoices);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== HELPER: Populate Invoice Transient Fields ====================
    private void populateInvoiceTransientFields(VendorInvoice invoice) {
        // Populate invoice items with itemName
        List<VendorInvoiceItem> items = vendorInvoiceItemRepo.findByInvoiceId(invoice.getId());
        for (VendorInvoiceItem item : items) {
            if (item.getGrnItemId() != null) {
                grnItemsRepo.findById(item.getGrnItemId()).ifPresent(grnItem -> {
                    if (grnItem.getItemId() != null) {
                        itemsRepo.findById(grnItem.getItemId())
                                .ifPresent(itemEntity -> item.setItemName(itemEntity.getName()));
                    }
                });
            }
        }
        invoice.setInvoiceItemList(items);

        // Populate projectName
        if (invoice.getProjectId() != null) {
            projectRepo.findById(invoice.getProjectId())
                    .ifPresent(project -> invoice.setProjectName(project.getName()));
        }

        // Populate vendorName
        if (invoice.getVendorId() != null) {
            vendorAccountRepo.findById(invoice.getVendorId())
                    .ifPresent(vendor -> invoice.setVendorName(vendor.getName()));
        }

        // Populate poNumber
        if (invoice.getPoId() != null) {
            poRepository.findById(invoice.getPoId())
                    .ifPresent(po -> invoice.setPoNumber(po.getPoNumber()));
        }

        // Populate grnNumber
        if (invoice.getGrnId() != null) {
            grnRepo.findById(invoice.getGrnId())
                    .ifPresent(grn -> invoice.setGrnNumber(grn.getGrnNumber()));
        }
    }

    // ==================== 6. UPDATE INVOICE (ONLY UNPAID) ====================
    @Transactional
    public Map<String, Object> updateInvoice(Long invoiceId, VendorInvoice invoiceInput, String loggedInUser) {
        LocalDateTime now = LocalDateTime.now();

        try {
            // ===========================
            // 1️⃣ Fetch Existing Invoice
            // ===========================
            VendorInvoice existingInvoice = vendorInvoiceRepo.findById(invoiceId)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

            // ===========================
            // 2️⃣ Validate Invoice Status - Only UNPAID can be updated
            // ===========================
            if (existingInvoice.getStatus() != InvoiceStatus.UNPAID) {
                throw new IllegalArgumentException("Only UNPAID invoices can be updated. Current status: " + existingInvoice.getStatus());
            }

            // ===========================
            // 3️⃣ Basic Validations
            // ===========================
            ValidationService.validate(invoiceInput.getTotalAmount(), "Total Amount");

            if (invoiceInput.getInvoiceItemList() == null || invoiceInput.getInvoiceItemList().isEmpty()) {
                throw new IllegalArgumentException("Invoice must contain at least one item");
            }

            // ===========================
            // 4️⃣ Fetch and Validate GRN
            // ===========================
            Grn grn = grnRepo.findById(existingInvoice.getGrnId())
                    .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

            // ===========================
            // 5️⃣ Rollback Previous Invoice Quantities from GRN Items
            // ===========================
            List<VendorInvoiceItem> oldInvoiceItems = vendorInvoiceItemRepo.findByInvoiceId(invoiceId);
            for (VendorInvoiceItem oldItem : oldInvoiceItems) {
                GrnItems grnItem = grnItemsRepo.findById(oldItem.getGrnItemId())
                        .orElseThrow(() -> new IllegalArgumentException("GRN Item not found"));

                // Reduce the invoiced quantity
                Double currentInvoiced = grnItem.getQuantityInvoiced() != null ? grnItem.getQuantityInvoiced() : 0.0;
                grnItem.setQuantityInvoiced(currentInvoiced - oldItem.getQuantity());
                grnItem.setUpdatedBy(loggedInUser);
                grnItem.setUpdatedDate(now);
                grnItemsRepo.save(grnItem);
            }

            // ===========================
            // 6️⃣ Delete Old Invoice Items
            // ===========================
            vendorInvoiceItemRepo.deleteAll(oldInvoiceItems);

            // ===========================
            // 7️⃣ Validate New Invoice Quantities Against GRN
            // ===========================
            List<GrnItems> grnItems = grnItemsRepo.findByGrnId(grn.getId());
            Map<Long, GrnItems> grnItemMap = new HashMap<>();
            for (GrnItems item : grnItems) {
                grnItemMap.put(item.getId(), item);
            }

            double calculatedTotal = 0;

            for (VendorInvoiceItem invoiceItem : invoiceInput.getInvoiceItemList()) {
                ValidationService.validate(invoiceItem.getGrnItemId(), "GRN Item Id");
                ValidationService.validate(invoiceItem.getQuantity(), "Quantity");
                ValidationService.validate(invoiceItem.getRate(), "Rate");

                if (invoiceItem.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Invoice quantity must be greater than 0");
                }

                GrnItems grnItem = grnItemMap.get(invoiceItem.getGrnItemId());
                if (grnItem == null) {
                    throw new IllegalArgumentException("GRN Item not found with id: " + invoiceItem.getGrnItemId());
                }

                // Calculate already invoiced quantity for this GRN item (after rollback)
                Double alreadyInvoiced = vendorInvoiceItemRepo.getTotalInvoicedQuantityByGrnItemId(invoiceItem.getGrnItemId());
                if (alreadyInvoiced == null) alreadyInvoiced = 0.0;
                Double pendingInvoiceQty = grnItem.getQuantityReceived() - alreadyInvoiced;

                if (invoiceItem.getQuantity() > pendingInvoiceQty) {
                    throw new IllegalArgumentException("Invoice quantity (" + invoiceItem.getQuantity() +
                            ") exceeds pending quantity (" + pendingInvoiceQty + ") for GRN Item: " + invoiceItem.getGrnItemId());
                }

                calculatedTotal += invoiceItem.getQuantity() * invoiceItem.getRate();
            }

            // ===========================
            // 8️⃣ Update Invoice Header
            // ===========================
            if (invoiceInput.getInvoiceNumber() != null && !invoiceInput.getInvoiceNumber().isEmpty()) {
                existingInvoice.setInvoiceNumber(invoiceInput.getInvoiceNumber());
            }
            existingInvoice.setTotalAmount(invoiceInput.getTotalAmount());
            existingInvoice.setPendingAmount(invoiceInput.getTotalAmount() - existingInvoice.getPaidAmount());

            if (invoiceInput.getInvoiceDate() != null) {
                existingInvoice.setInvoiceDate(invoiceInput.getInvoiceDate());
            }
            if (invoiceInput.getDueDate() != null) {
                existingInvoice.setDueDate(invoiceInput.getDueDate());
            }

            existingInvoice.setUpdatedBy(loggedInUser);
            existingInvoice.setUpdatedDate(now);
            vendorInvoiceRepo.save(existingInvoice);

            // ===========================
            // 9️⃣ Save New Invoice Items and Update GRN Item Invoiced Quantities
            // ===========================
            for (VendorInvoiceItem invoiceItem : invoiceInput.getInvoiceItemList()) {
                GrnItems grnItem = grnItemMap.get(invoiceItem.getGrnItemId());

                VendorInvoiceItem newInvoiceItem = new VendorInvoiceItem();
                newInvoiceItem.setInvoiceId(existingInvoice.getId());
                newInvoiceItem.setGrnItemId(invoiceItem.getGrnItemId());
                newInvoiceItem.setQuantity(invoiceItem.getQuantity());
                newInvoiceItem.setRate(invoiceItem.getRate());
                newInvoiceItem.setAmount(invoiceItem.getQuantity() * invoiceItem.getRate());
                newInvoiceItem.setCreatedBy(loggedInUser);
                newInvoiceItem.setUpdatedBy(loggedInUser);
                newInvoiceItem.setCreatedDate(now);
                newInvoiceItem.setUpdatedDate(now);

                vendorInvoiceItemRepo.save(newInvoiceItem);

                // Update invoiced quantity in GRN item
                Double currentInvoiced = grnItem.getQuantityInvoiced() != null ? grnItem.getQuantityInvoiced() : 0.0;
                grnItem.setQuantityInvoiced(currentInvoiced + invoiceItem.getQuantity());
                grnItem.setUpdatedBy(loggedInUser);
                grnItem.setUpdatedDate(now);
                grnItemsRepo.save(grnItem);
            }

// ===========================
// 🔟 Fetch New Saved Invoice Items For Accounting
// ===========================
            List<VendorInvoiceItem> newInvoiceItems =
                    vendorInvoiceItemRepo.findByInvoiceId(existingInvoice.getId());

// ===========================
// 1️⃣1️⃣ Create Accounting Journal Entry For Invoice Update
// ===========================
            journalEntryService.updateJournalEntryForVendorInvoice(
                    existingInvoice,
                    oldInvoiceItems,
                    newInvoiceItems,
                    loggedInUser
            );

// ===========================
// 1️⃣2️⃣ Calculate and update GRN invoice status based on all items
// ===========================
            grnService.calculateAndUpdateGrnInvoiceStatus(grn.getId(), loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Invoice updated successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 7. GET PENDING AMOUNT BY VENDOR ====================
    public Map<String, Object> getPendingAmountByVendor(Long vendorId) {
        try {
            Double pendingAmount = vendorInvoiceRepo.getPendingAmountByVendorId(vendorId);
            Map<String, Object> result = new HashMap<>();
            result.put("vendorId", vendorId);
            result.put("pendingAmount", pendingAmount);

            return ResponseMapper.buildResponse(Responses.SUCCESS, result);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== HELPER: Generate Invoice Number ====================
    public String generateInvoiceNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Optional<VendorInvoice> lastInvoiceOpt = vendorInvoiceRepo.findTopByOrderByIdDesc();

        int nextSequence = 1;
        if (lastInvoiceOpt.isPresent()) {
            String lastInvoiceNumber = lastInvoiceOpt.get().getInvoiceNumber();
            if (lastInvoiceNumber != null && lastInvoiceNumber.startsWith("INV-")) {
                String[] parts = lastInvoiceNumber.split("-");
                if (parts.length == 3 && parts[1].equals(datePart)) {
                    nextSequence = Integer.parseInt(parts[2]) + 1;
                }
            }
        }

        return String.format("INV-%s-%03d", datePart, nextSequence);
    }
}
