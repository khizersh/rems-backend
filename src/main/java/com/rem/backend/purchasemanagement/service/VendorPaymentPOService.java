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

// new imports
import com.rem.backend.accountmanagement.entity.OrganizationAccount;
import com.rem.backend.accountmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.repository.OrganizationAccoutRepo;
import com.rem.backend.repository.OrganizationAccountDetailRepo;
import com.rem.backend.enums.TransactionType;
import com.rem.backend.accountmanagement.enums.TransactionCategory;

@Service
@RequiredArgsConstructor
public class VendorPaymentPOService {

    private final VendorPaymentPORepo vendorPaymentPORepo;
    private final VendorInvoiceRepo vendorInvoiceRepo;

    // new repos
    private final OrganizationAccoutRepo organizationAccountRepo;
    private final OrganizationAccountDetailRepo organizationAccountDetailRepo;

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
            // 5️⃣ Deduct Organization Account Balance & Insert Account Detail
            // ===========================
            // Use organizationAccountId from request and validate it belongs to the invoice organization
            ValidationService.validate(paymentInput.getOrganizationAccountId(), "organization account");

            OrganizationAccount orgAccount = organizationAccountRepo
                    .findByIdAndOrganizationId(paymentInput.getOrganizationAccountId(), invoice.getOrgId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Organization Account for this organization"));

            double currentBalance = orgAccount.getTotalAmount();
            double paymentAmount = payment.getAmount() != null ? payment.getAmount() : 0.0;

            if (currentBalance < paymentAmount) {
                throw new IllegalArgumentException("Insufficient organization account balance");
            }

            double newBalance = currentBalance - paymentAmount;
            orgAccount.setTotalAmount(newBalance);
            orgAccount.setUpdatedBy(loggedInUser);
            organizationAccountRepo.save(orgAccount);

            OrganizationAccountDetail detail = new OrganizationAccountDetail();
            detail.setOrganizationAcctId(orgAccount.getId());
            detail.setTransactionType(TransactionType.CREDIT);
            detail.setTransactionCategory(TransactionCategory.OTHER);
            detail.setAmount(paymentAmount);
            detail.setComments("Vendor Payment: invoiceId=" + invoice.getId() + " paymentId=" + payment.getId());
            detail.setProjectId(payment.getProjectId() != null ? payment.getProjectId().intValue() : 0);
            // set created/updated by
            detail.setCreatedBy(loggedInUser);
            detail.setUpdatedBy(loggedInUser);

            organizationAccountDetailRepo.save(detail);

            // persist which organization account was used
            payment.setOrganizationAccountId(orgAccount.getId());
            payment.setUpdatedDate(LocalDateTime.now());
            vendorPaymentPORepo.save(payment);

            // ===========================
            // 6️⃣ Auto-calculate Pending Amount and Update Invoice Status
            // ===========================
            updateInvoiceAfterPayment(invoice, paymentInput.getAmount(), loggedInUser);

            Map<String, Object> result = new HashMap<>();
            result.put("payment", payment);
            result.put("invoiceStatus", invoice.getStatus());
            result.put("pendingAmount", invoice.getPendingAmount());
            result.put("orgAccountBalance", newBalance);
            result.put("organizationAccountId", orgAccount.getId());

            return ResponseMapper.buildResponse(Responses.SUCCESS, result);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ==================== UPDATE VENDOR PAYMENT ====================
    @Transactional
    public Map<String, Object> updatePayment(Long paymentId, VendorPaymentPO request, String loggedInUser) {
        try {
            ValidationService.validate(paymentId, "paymentId");
            ValidationService.validate(request.getAmount(), "amount");

            VendorPaymentPO existing = vendorPaymentPORepo.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

            double oldAmount = existing.getAmount() != null ? existing.getAmount() : 0.0;
            double newAmount = request.getAmount() != null ? request.getAmount() : 0.0;
            double delta = newAmount - oldAmount; // positive => more deducted; negative => refund

            Long oldOrgAcctId = existing.getOrganizationAccountId();
            Long newOrgAcctId = request.getOrganizationAccountId();

            OrganizationAccount oldOrgAcct = null;
            OrganizationAccount newOrgAcct = null;

            if (oldOrgAcctId != null && oldOrgAcctId != 0) {
                oldOrgAcct = organizationAccountRepo.findById(oldOrgAcctId)
                        .orElseThrow(() -> new IllegalArgumentException("Old organization account not found"));
            }
            if (newOrgAcctId != null && newOrgAcctId != 0) {
                newOrgAcct = organizationAccountRepo.findById(newOrgAcctId)
                        .orElseThrow(() -> new IllegalArgumentException("New organization account not found"));
            }

            // if account changed, refund old and deduct new
            if (oldOrgAcctId != null && !Objects.equals(oldOrgAcctId, newOrgAcctId)) {
                if (oldOrgAcct != null) {
                    oldOrgAcct.setTotalAmount(oldOrgAcct.getTotalAmount() + oldAmount);
                    oldOrgAcct.setUpdatedBy(loggedInUser);
                    organizationAccountRepo.save(oldOrgAcct);

                    OrganizationAccountDetail refundDetail = new OrganizationAccountDetail();
                    refundDetail.setOrganizationAcctId(oldOrgAcct.getId());
                    refundDetail.setAmount(oldAmount);
                    refundDetail.setComments("Refund from organization account for payment update (invoice: " + existing.getInvoiceId() + ")");
                    refundDetail.setTransactionType(TransactionType.DEBIT);
                    refundDetail.setCreatedBy(loggedInUser);
                    refundDetail.setUpdatedBy(loggedInUser);
                    organizationAccountDetailRepo.save(refundDetail);
                }

                if (newOrgAcct != null) {
                    if (newOrgAcct.getTotalAmount() < newAmount) {
                        throw new IllegalArgumentException("Insufficient funds in the new organization account");
                    }
                    newOrgAcct.setTotalAmount(newOrgAcct.getTotalAmount() - newAmount);
                    newOrgAcct.setUpdatedBy(loggedInUser);
                    organizationAccountRepo.save(newOrgAcct);

                    OrganizationAccountDetail deduct = new OrganizationAccountDetail();
                    deduct.setOrganizationAcctId(newOrgAcct.getId());
                    deduct.setAmount(newAmount);
                    deduct.setComments("Deduct for updated vendor payment (invoice: " + existing.getInvoiceId() + ")");
                    deduct.setTransactionType(TransactionType.CREDIT);
                    deduct.setCreatedBy(loggedInUser);
                    deduct.setUpdatedBy(loggedInUser);
                    organizationAccountDetailRepo.save(deduct);
                }

            } else {
                // same account
                OrganizationAccount target = newOrgAcct != null ? newOrgAcct : oldOrgAcct;
                if (target != null && delta != 0) {
                    if (delta > 0) { // need to deduct extra
                        if (target.getTotalAmount() < delta) {
                            throw new IllegalArgumentException("Insufficient funds in the organization account for increased amount");
                        }
                        target.setTotalAmount(target.getTotalAmount() - delta);
                        target.setUpdatedBy(loggedInUser);
                        organizationAccountRepo.save(target);

                        OrganizationAccountDetail deduct = new OrganizationAccountDetail();
                        deduct.setOrganizationAcctId(target.getId());
                        deduct.setAmount(delta);
                        deduct.setComments("Deduct for increased vendor payment (invoice: " + existing.getInvoiceId() + ")");
                        deduct.setTransactionType(TransactionType.CREDIT);
                        deduct.setCreatedBy(loggedInUser);
                        deduct.setUpdatedBy(loggedInUser);
                        organizationAccountDetailRepo.save(deduct);
                    } else { // refund
                        double refundAmt = -delta;
                        target.setTotalAmount(target.getTotalAmount() + refundAmt);
                        target.setUpdatedBy(loggedInUser);
                        organizationAccountRepo.save(target);

                        OrganizationAccountDetail refund = new OrganizationAccountDetail();
                        refund.setOrganizationAcctId(target.getId());
                        refund.setAmount(refundAmt);
                        refund.setComments("Refund for decreased vendor payment (invoice: " + existing.getInvoiceId() + ")");
                        refund.setTransactionType(TransactionType.DEBIT);
                        refund.setCreatedBy(loggedInUser);
                        refund.setUpdatedBy(loggedInUser);
                        organizationAccountDetailRepo.save(refund);
                    }
                }
            }

            // Update payment
            existing.setAmount(newAmount);
            existing.setOrganizationAccountId(newOrgAcctId != null ? newOrgAcctId : oldOrgAcctId);
            existing.setUpdatedBy(loggedInUser);
            existing.setUpdatedDate(LocalDateTime.now());
            vendorPaymentPORepo.save(existing);

            // adjust invoice paid/pending by delta
            VendorInvoice invoice = vendorInvoiceRepo.findById(existing.getInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

            adjustInvoiceForUpdatedPayment(invoice, delta, loggedInUser);

            Map<String, Object> resp = new HashMap<>();
            resp.put("payment", existing);
            resp.put("invoiceStatus", invoice.getStatus());
            resp.put("pendingAmount", invoice.getPendingAmount());
            if (existing.getOrganizationAccountId() != null) {
                organizationAccountRepo.findById(existing.getOrganizationAccountId()).ifPresent(acct -> resp.put("orgAccountBalance", acct.getTotalAmount()));
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, resp);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    private void adjustInvoiceForUpdatedPayment(VendorInvoice invoice, double delta, String loggedInUser) {
        double paid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : 0.0;
        paid = paid + delta;
        if (paid < 0) paid = 0.0;
        double pending = invoice.getTotalAmount() - paid;
        if (pending < 0) pending = 0.0;

        invoice.setPaidAmount(paid);
        invoice.setPendingAmount(pending);
        if (pending == 0.0) invoice.setStatus(InvoiceStatus.PAID);
        else if (paid > 0.0) invoice.setStatus(InvoiceStatus.PARTIAL);

        invoice.setUpdatedBy(loggedInUser);
        invoice.setUpdatedDate(LocalDateTime.now());
        vendorInvoiceRepo.save(invoice);
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
        double currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : 0.0;
        double newPaidAmount = currentPaid + (paymentAmount != null ? paymentAmount : 0.0);
        double newPendingAmount = invoice.getTotalAmount() - newPaidAmount;

        invoice.setPaidAmount(newPaidAmount);
        invoice.setPendingAmount(newPendingAmount);

        // Auto-update status based on payment
        if (newPendingAmount <= 0.0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPendingAmount(0.0);
        } else if (newPaidAmount > 0.0) {
            invoice.setStatus(InvoiceStatus.PARTIAL);
        }

        invoice.setUpdatedBy(loggedInUser);
        invoice.setUpdatedDate(LocalDateTime.now());
        vendorInvoiceRepo.save(invoice);
    }
}
