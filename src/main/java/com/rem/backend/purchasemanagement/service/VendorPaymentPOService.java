package com.rem.backend.purchasemanagement.service;

import com.rem.backend.purchasemanagement.entity.vendorinvoice.VendorInvoice;
import com.rem.backend.purchasemanagement.entity.vendorpayment.VendorPaymentPO;
import com.rem.backend.purchasemanagement.enums.InvoiceStatus;
import com.rem.backend.purchasemanagement.repository.VendorInvoiceRepo;
import com.rem.backend.purchasemanagement.repository.VendorPaymentPORepo;
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
import java.util.*;

@Service
@RequiredArgsConstructor
public class VendorPaymentPOService {

    private final VendorPaymentPORepo vendorPaymentPORepo;
    private final VendorInvoiceRepo vendorInvoiceRepo;

    // ==================== 1. CREATE VENDOR PAYMENT ====================
    @Transactional
    public Map<String, Object> createPayment(VendorPaymentPO paymentInput, String loggedInUser) {

        LocalDateTime now = LocalDateTime.now();

        try {
            // ===========================
            // 1️⃣ Basic Payment Validations
            // ===========================
            ValidationService.validate(paymentInput.getInvoiceId(), "Invoice Id");
            ValidationService.validate(paymentInput.getAmount(), "Amount");

            if (paymentInput.getAmount() <= 0) {
                throw new IllegalArgumentException("Payment amount must be greater than 0");
            }

            // ===========================
            // 2️⃣ Fetch and Validate Invoice
            // ===========================
            VendorInvoice invoice = vendorInvoiceRepo.findById(paymentInput.getInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                throw new IllegalArgumentException("Invoice is already fully paid");
            }

            // ===========================
            // 3️⃣ Validate Payment Amount Against Pending Amount
            // ===========================
            Double pendingAmount = invoice.getPendingAmount() != null ? invoice.getPendingAmount() :
                    (invoice.getTotalAmount() - (invoice.getPaidAmount() != null ? invoice.getPaidAmount() : 0.0));

            if (paymentInput.getAmount() > pendingAmount) {
                throw new IllegalArgumentException("Payment amount (" + paymentInput.getAmount() +
                        ") exceeds pending amount (" + pendingAmount + ")");
            }

            // ===========================
            // 4️⃣ Create Payment
            // ===========================
            VendorPaymentPO payment = new VendorPaymentPO();
            payment.setOrgId(invoice.getOrgId());
            payment.setProjectId(invoice.getProjectId());
            payment.setVendorId(invoice.getVendorId());
            payment.setInvoiceId(invoice.getId());
            payment.setAmount(paymentInput.getAmount());
            payment.setPaymentMode(paymentInput.getPaymentMode());
            payment.setReferenceNumber(paymentInput.getReferenceNumber());
            payment.setPaymentDate(paymentInput.getPaymentDate() != null ? paymentInput.getPaymentDate() : LocalDate.now());
            payment.setRemarks(paymentInput.getRemarks());
            payment.setCreatedBy(loggedInUser);
            payment.setUpdatedBy(loggedInUser);
            payment.setCreatedDate(now);
            payment.setUpdatedDate(now);

            payment = vendorPaymentPORepo.save(payment);

            // ===========================
            // 5️⃣ Auto-calculate Pending Amount and Update Invoice Status
            // ===========================
            updateInvoiceAfterPayment(invoice, paymentInput.getAmount(), loggedInUser);

            Map<String, Object> result = new HashMap<>();
            result.put("payment", payment);
            result.put("invoiceStatus", invoice.getStatus());
            result.put("pendingAmount", invoice.getPendingAmount());

            return ResponseMapper.buildResponse(Responses.SUCCESS, result);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 2. GET PAYMENT BY ID ====================
    public Map<String, Object> getById(Long paymentId) {
        try {
            VendorPaymentPO payment = vendorPaymentPORepo.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

            return ResponseMapper.buildResponse(Responses.SUCCESS, payment);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 3. GET PAYMENTS BY INVOICE ====================
    public Map<String, Object> getByInvoiceId(Long invoiceId) {
        try {
            List<VendorPaymentPO> payments = vendorPaymentPORepo.findByInvoiceId(invoiceId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, payments);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 4. GET PAYMENTS BY VENDOR ====================
    public Map<String, Object> getByVendorId(Long vendorId, Pageable pageable) {
        try {
            Page<VendorPaymentPO> payments = vendorPaymentPORepo.findByVendorId(vendorId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, payments);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 5. GET ALL PAYMENTS BY ORGANIZATION ====================
    public Map<String, Object> getAllByOrgId(Long orgId, Pageable pageable) {
        try {
            Page<VendorPaymentPO> payments = vendorPaymentPORepo.findByOrgId(orgId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, payments);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== 6. GET TOTAL PAID AMOUNT FOR INVOICE ====================
    public Map<String, Object> getTotalPaidForInvoice(Long invoiceId) {
        try {
            Double totalPaid = vendorPaymentPORepo.getTotalPaidAmountByInvoiceId(invoiceId);

            VendorInvoice invoice = vendorInvoiceRepo.findById(invoiceId)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

            Map<String, Object> result = new HashMap<>();
            result.put("invoiceId", invoiceId);
            result.put("totalAmount", invoice.getTotalAmount());
            result.put("paidAmount", totalPaid);
            result.put("pendingAmount", invoice.getTotalAmount() - totalPaid);
            result.put("status", invoice.getStatus());

            return ResponseMapper.buildResponse(Responses.SUCCESS, result);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== HELPER: Update Invoice After Payment ====================
    private void updateInvoiceAfterPayment(VendorInvoice invoice, Double paymentAmount, String loggedInUser) {
        Double currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : 0.0;
        Double newPaidAmount = currentPaid + paymentAmount;
        Double newPendingAmount = invoice.getTotalAmount() - newPaidAmount;

        invoice.setPaidAmount(newPaidAmount);
        invoice.setPendingAmount(newPendingAmount);

        // Auto-update status based on payment
        if (newPendingAmount <= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPendingAmount(0.0);
        } else if (newPaidAmount > 0) {
            invoice.setStatus(InvoiceStatus.PARTIAL);
        }

        invoice.setUpdatedBy(loggedInUser);
        invoice.setUpdatedDate(LocalDateTime.now());
        vendorInvoiceRepo.save(invoice);
    }
}
