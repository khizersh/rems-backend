package com.rem.backend.service;

import com.rem.backend.accountmanagement.entity.OrganizationAccount;
import com.rem.backend.accountmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.accountmanagement.enums.TransactionCategory;
import com.rem.backend.accountmanagement.service.OrganizationAccountService;
import com.rem.backend.dto.expense.PdcListRequestDTO;
import com.rem.backend.entity.expense.Expense;
import com.rem.backend.entity.expense.ExpenseDetail;
import com.rem.backend.entity.vendor.VendorAccount;
import com.rem.backend.entity.vendor.VendorPayment;
import com.rem.backend.enums.*;
import com.rem.backend.repository.*;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class PdcPaymentService {

    private final ExpenseRepo expenseRepo;
    private final OrganizationAccoutRepo organizationAccountRepo;
    private final OrganizationAccountDetailRepo organizationAccountDetailRepo;
    private final OrganizationAccountService organizationAccountService;
    private final VendorAccountRepo vendorAccountRepo;
    private final VendorAccountService vendorAccountService;
    private final VendorAccountDetailRepo vendorAccountDetailRepo;
    private final ExpenseDetailRepo expenseDetailRepo;
    private final JournalEntryService journalEntryService;


    // ──────────────────────────────────────────────────────────────────────
    // 1. Process (Clear) a PDC
    // POST /api/payments/process-pdc/{id}
    // ──────────────────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> processPdc(long expenseId, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(expenseId, "expense id");

            // 1. Fetch PDC expense
            Optional<Expense> expenseOpt = expenseRepo.findById(expenseId);
            if (expenseOpt.isEmpty())
                throw new IllegalArgumentException("PDC transaction not found");

            Expense expense = expenseOpt.get();

            // 2. Validate it is a PDC
            if (expense.getPaymentMode() == null || !expense.getPaymentMode().equals(PaymentMode.PDC))
                throw new IllegalArgumentException("This transaction is not a PDC payment");

            // 3. Prevent re-processing
            if (expense.getPdcStatus() != null && expense.getPdcStatus().equals(PdcStatus.CLEARED))
                throw new IllegalArgumentException("This PDC has already been cleared");

            if (expense.getPdcStatus() != null && expense.getPdcStatus().equals(PdcStatus.FAILED))
                throw new IllegalArgumentException("This PDC has been marked as failed. Create a new expense instead.");

            // 4. Validate organization account balance
            Optional<OrganizationAccount> orgAccountOpt = organizationAccountRepo.findById(expense.getOrganizationAccountId());
            if (orgAccountOpt.isEmpty())
                throw new IllegalArgumentException("Organization account not found");

            OrganizationAccount organizationAccount = orgAccountOpt.get();
            double remainingAmount = organizationAccount.getTotalAmount() - expense.getAmountPaid();

            if (remainingAmount < 0)
                throw new IllegalArgumentException("Insufficient balance in account: " + organizationAccount.getName()
                        + ". Available: " + organizationAccount.getTotalAmount()
                        + ", Required: " + expense.getAmountPaid());

            // 5. Deduct from organization account
            OrganizationAccountDetail orgAcctDetail = new OrganizationAccountDetail();
            orgAcctDetail.setExpenseId(expense.getId());
            orgAcctDetail.setComments("PDC Clearance - Cheque #" + expense.getChequeNumber()
                    + " | " + expense.getExpenseTitle());
            orgAcctDetail.setAmount(expense.getAmountPaid());
            orgAcctDetail.setTransactionCategory(TransactionCategory.CUSTOMER_PAYMENT);
            orgAcctDetail.setOrganizationAcctId(expense.getOrganizationAccountId());
            orgAcctDetail.setProjectId(expense.getProjectId() != null ? expense.getProjectId() : 0);

            organizationAccount = organizationAccountService.deductFromOrgAcct(orgAcctDetail, loggedInUser);

            // 6. Update PDC status to CLEARED
            expense.setPdcStatus(PdcStatus.CLEARED);
            expense.setPaymentStatus(PaymentStatus.PAID);
            expense.setUpdatedBy(loggedInUser);
            expenseRepo.save(expense);

            // 7. Create expense detail record
            ExpenseDetail expenseDetail = new ExpenseDetail();
            expenseDetail.setExpenseId(expense.getId());
            expenseDetail.setAmountPaid(expense.getAmountPaid());
            expenseDetail.setOrganizationAccountId(expense.getOrganizationAccountId());
            expenseDetail.setOrganizationAccountTitle(organizationAccount.getName());
            expenseDetail.setExpenseTitle(expense.getExpenseTitle());
            expenseDetail.setPaymentType(PaymentType.CHEQUE);
            expenseDetail.setPaymentDocNo(expense.getChequeNumber());
            expenseDetail.setPaymentDocDate(expense.getChequeDate() != null ? expense.getChequeDate().atStartOfDay() : null);
            expenseDetail.setCreatedBy(loggedInUser);
            expenseDetail.setUpdatedBy(loggedInUser);
            expenseDetailRepo.save(expenseDetail);

            // 8. Vendor ledger impact — reduce payable on clearance
            if (expense.getExpenseType() != null
                    && expense.getExpenseType().equals(ExpenseType.CONSTRUCTION)
                    && expense.getVendorAccountId() != null
                    && expense.getVendorAccountId() > 0) {

                Optional<VendorAccount> vendorOpt = vendorAccountRepo.findById(expense.getVendorAccountId());
                if (vendorOpt.isPresent()) {
                    VendorAccount vendorAccount = vendorOpt.get();
                    vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + expense.getAmountPaid());
                    double updatedCreditBalance = vendorAccount.getTotalCreditAmount() - expense.getCreditAmount();
                    vendorAccount.setTotalCreditAmount(Math.max(updatedCreditBalance, 0));
                    vendorAccount.setUpdatedBy(loggedInUser);
                    vendorAccountRepo.save(vendorAccount);

                    // Create vendor payment record
                    VendorPayment vendorPayment = new VendorPayment();
                    vendorPayment.setAmountPaid(expense.getAmountPaid());
                    vendorPayment.setOrganizationAccountId(expense.getOrganizationAccountId());
                    vendorPayment.setCreditAmount(0);
                    vendorPayment.setBalanceAmount(Math.max(updatedCreditBalance, 0));
                    vendorPayment.setProjectId(expense.getProjectId() != null ? expense.getProjectId() : 0);
                    vendorPayment.setTransactionType(TransactionType.DEBIT);
                    vendorPayment.setVendorPaymentType(VendorPaymentType.DIRECT_PURCHASE);
                    vendorPayment.setVendorAccountId(expense.getVendorAccountId());
                    vendorPayment.setExpenseId(expense.getId());
                    vendorPayment.setComments("PDC Cleared - Cheque #" + expense.getChequeNumber());
                    vendorPayment.setPaymentMethodType(PaymentType.CHEQUE);
                    vendorPayment.setPaymentDocNo(expense.getChequeNumber());
                    vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);
                }
            }

            // 9. Create journal entry for PDC clearance (Dr Expense/Payable, Cr Bank)
            journalEntryService.createJournalEntryForExpense(expense, organizationAccount, loggedInUser);

            log.info("PDC {} cleared successfully by {}", expenseId, loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, expense);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error processing PDC {}: {}", expenseId, e.getMessage(), e);
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // ──────────────────────────────────────────────────────────────────────
    // 2. Mark PDC as Failed
    // POST /api/payments/mark-failed/{id}
    // ──────────────────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> markFailed(long expenseId, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(expenseId, "expense id");

            Optional<Expense> expenseOpt = expenseRepo.findById(expenseId);
            if (expenseOpt.isEmpty())
                throw new IllegalArgumentException("PDC transaction not found");

            Expense expense = expenseOpt.get();

            if (expense.getPaymentMode() == null || !expense.getPaymentMode().equals(PaymentMode.PDC))
                throw new IllegalArgumentException("This transaction is not a PDC payment");

            if (expense.getPdcStatus() != null && expense.getPdcStatus().equals(PdcStatus.CLEARED))
                throw new IllegalArgumentException("Cannot mark a cleared PDC as failed");

            if (expense.getPdcStatus() != null && expense.getPdcStatus().equals(PdcStatus.FAILED))
                throw new IllegalArgumentException("This PDC is already marked as failed");

            // Mark as failed — no balance impact
            expense.setPdcStatus(PdcStatus.FAILED);
            expense.setPaymentStatus(PaymentStatus.UNPAID);
            expense.setUpdatedBy(loggedInUser);
            expenseRepo.save(expense);

            log.info("PDC {} marked as FAILED by {}", expenseId, loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, expense);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error marking PDC {} as failed: {}", expenseId, e.getMessage(), e);
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // ──────────────────────────────────────────────────────────────────────
    // 3. List PDC Payments with filters
    // POST /api/payments/pdc/list
    // ──────────────────────────────────────────────────────────────────────
    public Map<String, Object> listPdcPayments(PdcListRequestDTO request, Pageable pageable) {
        try {
            ValidationService.validate(request.getOrganizationId(), "organization id");

            long orgId = request.getOrganizationId();
            LocalDate today = LocalDate.now();
            Page<Expense> result;

            String filter = request.getFilter() != null ? request.getFilter().toLowerCase() : "all";

            switch (filter) {
                case "duetoday":
                    result = expenseRepo.findPdcDueToday(orgId, today, pageable);
                    break;

                case "overdue":
                    result = expenseRepo.findPdcOverdue(orgId, today, pageable);
                    break;

                case "upcoming":
                    result = expenseRepo.findPdcUpcoming(orgId, today, pageable);
                    break;

                case "all":
                default:
                    if (request.getVendorAccountId() != null && request.getVendorAccountId() > 0) {
                        result = expenseRepo.findAllByOrganizationIdAndPaymentModeAndPdcStatusAndVendorAccountId(
                                orgId, PaymentMode.PDC, PdcStatus.PENDING, request.getVendorAccountId(), pageable);
                    } else if (request.getProjectId() != null && request.getProjectId() > 0) {
                        result = expenseRepo.findAllByOrganizationIdAndPaymentModeAndPdcStatusAndProjectId(
                                orgId, PaymentMode.PDC, PdcStatus.PENDING, request.getProjectId(), pageable);
                    } else {
                        result = expenseRepo.findAllByOrganizationIdAndPaymentModeAndPdcStatus(
                                orgId, PaymentMode.PDC, PdcStatus.PENDING, pageable);
                    }
                    break;
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, result);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            log.error("Error listing PDC payments: {}", e.getMessage(), e);
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // ──────────────────────────────────────────────────────────────────────
    // 4. Get PDC by ID
    // GET /api/payments/pdc/{id}
    // ──────────────────────────────────────────────────────────────────────
    public Map<String, Object> getPdcById(long expenseId) {
        try {
            ValidationService.validate(expenseId, "expense id");

            Optional<Expense> expenseOpt = expenseRepo.findById(expenseId);
            if (expenseOpt.isEmpty())
                throw new IllegalArgumentException("PDC transaction not found");

            Expense expense = expenseOpt.get();
            if (expense.getPaymentMode() == null || !expense.getPaymentMode().equals(PaymentMode.PDC))
                throw new IllegalArgumentException("This transaction is not a PDC payment");

            return ResponseMapper.buildResponse(Responses.SUCCESS, expense);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
