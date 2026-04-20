package com.rem.backend.service;

import com.rem.backend.accountmanagement.entity.OrganizationAccount;
import com.rem.backend.accountmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.accountmanagement.enums.TransactionCategory;
import com.rem.backend.accountmanagement.service.OrganizationAccountService;
import com.rem.backend.dto.expense.PdcListRequestDTO;
import com.rem.backend.entity.expense.Expense;
import com.rem.backend.entity.expense.ExpenseDetail;
import com.rem.backend.entity.pdc.PdcRecord;
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.vendor.VendorAccount;
import com.rem.backend.entity.vendor.VendorPayment;
import com.rem.backend.enums.*;
import com.rem.backend.purchasemanagement.repository.ExpenseDetailRepo;
import com.rem.backend.purchasemanagement.repository.ExpenseRepo;
import com.rem.backend.purchasemanagement.repository.ExpenseTypeRepo;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class PdcPaymentService {

    private final PdcRecordRepo pdcRecordRepo;
    private final ExpenseRepo expenseRepo;
    private final OrganizationAccoutRepo organizationAccountRepo;
    private final OrganizationAccountService organizationAccountService;
    private final VendorAccountRepo vendorAccountRepo;
    private final VendorAccountService vendorAccountService;
    private final ExpenseDetailRepo expenseDetailRepo;
    private final JournalEntryService journalEntryService;
    private final ProjectRepo projectRepo;
    private final ExpenseTypeRepo expenseTypeRepo;


    // ──────────────────────────────────────────────────────────────────────
    // 1. Create PDC Record (No Expense Creation)
    // POST /api/pdc/create
    // ──────────────────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> createPdcRecord(PdcRecord pdcRecord, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(pdcRecord.getOrganizationId(), "organization id");
            ValidationService.validate(pdcRecord.getVendorAccountId(), "vendor account id");
            ValidationService.validate(pdcRecord.getOrganizationAccountId(), "organization account id");
            ValidationService.validate(pdcRecord.getAmount(), "amount");
            ValidationService.validate(pdcRecord.getChequeNumber(), "cheque number");
            ValidationService.validate(pdcRecord.getChequeDate(), "cheque date");

            // Validations
            if (pdcRecord.getChequeDate().isBefore(LocalDate.now()))
                throw new IllegalArgumentException("Cheque date must be today or a future date");
            if (pdcRecord.getAmount() <= 0)
                throw new IllegalArgumentException("Amount must be greater than 0");

            // Check for duplicate cheque number
            Optional<PdcRecord> existingPdc = pdcRecordRepo.findByChequeNumberAndOrganizationId(
                    pdcRecord.getChequeNumber(), pdcRecord.getOrganizationId());
            if (existingPdc.isPresent() && existingPdc.get().getStatus() == PdcStatus.PENDING)
                throw new IllegalArgumentException("A PDC with cheque number '" + pdcRecord.getChequeNumber() + "' already exists");

            // Validate vendor
            Optional<VendorAccount> vendorOpt = vendorAccountRepo.findById(pdcRecord.getVendorAccountId());
            if (vendorOpt.isEmpty())
                throw new IllegalArgumentException("Invalid vendor account");

            VendorAccount vendorAccount = vendorOpt.get();

            // Validate organization account
            Optional<OrganizationAccount> orgAcctOpt = organizationAccountRepo.findById(pdcRecord.getOrganizationAccountId());
            if (orgAcctOpt.isEmpty())
                throw new IllegalArgumentException("Invalid organization account");

            // Increase vendor payable (credit amount)
            vendorAccount.setTotalAmount(vendorAccount.getTotalAmount() + pdcRecord.getAmount());
            vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() + pdcRecord.getAmount());
            vendorAccount.setUpdatedBy(loggedInUser);
            vendorAccountRepo.save(vendorAccount);

            // Handle project details and increase project cost immediately
            if (pdcRecord.getProjectId() != null && pdcRecord.getProjectId() > 0) {
                Optional<Project> projectOpt = projectRepo.findById(pdcRecord.getProjectId());
                if (projectOpt.isPresent()) {
                    Project project = projectOpt.get();
                    pdcRecord.setProjectName(project.getName());

                    project.setConstructionAmount(project.getConstructionAmount() + pdcRecord.getAmount());
                    project.setTotalAmount(project.getTotalAmount() + pdcRecord.getAmount());
                    project.setUpdatedBy(loggedInUser);
                    projectRepo.save(project);
                    log.info("Project {} cost increased by {} due to PDC creation", project.getProjectId(), pdcRecord.getAmount());

                }
            }

            // Set PDC status
            pdcRecord.setStatus(PdcStatus.PENDING);
            pdcRecord.setCreatedBy(loggedInUser);
            pdcRecord.setUpdatedBy(loggedInUser);
            pdcRecord.setVendorName(vendorAccount.getName());
            pdcRecord.setOrgAccountTitle(orgAcctOpt.get().getName());

            pdcRecord = pdcRecordRepo.save(pdcRecord);

            // Create vendor payment record for PDC creation (for construction expenses)

            VendorPayment vendorPayment = new VendorPayment();
            vendorPayment.setVendorAccountId(pdcRecord.getVendorAccountId());
            vendorPayment.setOrganizationAccountId(pdcRecord.getOrganizationAccountId());
            vendorPayment.setProjectId(pdcRecord.getProjectId() != null ? pdcRecord.getProjectId() : 0);
            vendorPayment.setAmountPaid(0); // No amount paid yet
            vendorPayment.setCreditAmount(pdcRecord.getAmount());
            vendorPayment.setBalanceAmount(vendorAccount.getTotalCreditAmount());
            vendorPayment.setMaterialAmount(0);
            vendorPayment.setTransactionType(TransactionType.CREDIT);
            vendorPayment.setVendorPaymentType(VendorPaymentType.PDC_CREATION);
            vendorPayment.setPaymentMethodType(PaymentType.CHEQUE);
            vendorPayment.setComments("PDC Created - Cheque #" + pdcRecord.getChequeNumber() + " | " + pdcRecord.getTitle());
            vendorPayment.setPaymentDocNo(pdcRecord.getChequeNumber());
            vendorPayment.setPaymentDocDate(pdcRecord.getChequeDate() != null ? pdcRecord.getChequeDate().atStartOfDay() : null);
            vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);
            log.info("Vendor payment record created for PDC creation: PDC #{}", pdcRecord.getId());

            log.info("PDC record created: {} for vendor: {} by {}", pdcRecord.getId(), vendorAccount.getName(), loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, pdcRecord);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error creating PDC record: {}", e.getMessage(), e);
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // ──────────────────────────────────────────────────────────────────────
    // 2. Process (Clear) a PDC - Creates Expense
    // POST /api/pdc/process/{id}
    // ──────────────────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> processPdc(long pdcId, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(pdcId, "pdc id");

            // 1. Fetch PDC record
            Optional<PdcRecord> pdcOpt = pdcRecordRepo.findById(pdcId);
            if (pdcOpt.isEmpty())
                throw new IllegalArgumentException("PDC record not found");

            PdcRecord pdcRecord = pdcOpt.get();

            // 2. Prevent re-processing
            if (pdcRecord.getStatus().equals(PdcStatus.CLEARED))
                throw new IllegalArgumentException("This PDC has already been cleared");

            if (pdcRecord.getStatus().equals(PdcStatus.FAILED))
                throw new IllegalArgumentException("This PDC has been marked as failed. Cannot clear a failed PDC.");

            // 3. Validate organization account balance
            Optional<OrganizationAccount> orgAccountOpt = organizationAccountRepo.findById(pdcRecord.getOrganizationAccountId());
            if (orgAccountOpt.isEmpty())
                throw new IllegalArgumentException("Organization account not found");

            OrganizationAccount organizationAccount = orgAccountOpt.get();
            double remainingAmount = organizationAccount.getTotalAmount() - pdcRecord.getAmount();

            if (remainingAmount < 0)
                throw new IllegalArgumentException("Insufficient balance in account: " + organizationAccount.getName()
                        + ". Available: " + organizationAccount.getTotalAmount()
                        + ", Required: " + pdcRecord.getAmount());

            // 4. CREATE NEW EXPENSE (not update existing)
            Expense expense = new Expense();
            expense.setOrganizationId(pdcRecord.getOrganizationId());
            expense.setVendorAccountId(pdcRecord.getVendorAccountId());
            expense.setProjectId(pdcRecord.getProjectId());
            expense.setUnitId(pdcRecord.getUnitId());
            expense.setExpenseTypeId(pdcRecord.getExpenseTypeId());
            expense.setOrganizationAccountId(pdcRecord.getOrganizationAccountId());
            expense.setAmountPaid(pdcRecord.getAmount());
            expense.setCreditAmount(0);
            expense.setTotalAmount(pdcRecord.getAmount());
            expense.setPaymentStatus(PaymentStatus.PAID);
            expense.setPaymentType(PaymentType.CHEQUE);
            expense.setPdcStatus(PdcStatus.CLEARED);
            expense.setChequeNumber(pdcRecord.getChequeNumber());
            expense.setChequeDate(pdcRecord.getChequeDate());
            expense.setExpenseType(ExpenseType.CONSTRUCTION);
            expense.setBankName(pdcRecord.getBankName());
            expense.setExpenseTitle(pdcRecord.getTitle());
            expense.setComments(pdcRecord.getComments() != null ? pdcRecord.getComments() : "PDC Cleared - Cheque #" + pdcRecord.getChequeNumber());
            expense.setCreatedBy(loggedInUser);
            expense.setUpdatedBy(loggedInUser);

            // Get vendor and project names
            Optional<VendorAccount> vendorOpt = vendorAccountRepo.findById(pdcRecord.getVendorAccountId());
            if (vendorOpt.isPresent()) {
                expense.setVendorName(vendorOpt.get().getName());
            }

            if (pdcRecord.getProjectId() != null && pdcRecord.getProjectId() > 0) {
                Optional<Project> projectOpt = projectRepo.findById(pdcRecord.getProjectId());
                if (projectOpt.isPresent()) {
                    expense.setProjectName(projectOpt.get().getName());
                    // Note: Project cost was already increased during PDC creation
                }
            }

//            expense.setExpenseCOAId(journalEntryService.getConstructionInventoryControlAccount(pdcRecord.getOrganizationId()).getId());


            expense = expenseRepo.save(expense);
            expense.setOrgAccountTitle(organizationAccount.getName());

            // 5. Deduct from organization account
            OrganizationAccountDetail orgAcctDetail = new OrganizationAccountDetail();
            orgAcctDetail.setExpenseId(expense.getId());
            orgAcctDetail.setComments("PDC Clearance - Cheque #" + pdcRecord.getChequeNumber()
                    + " | " + expense.getExpenseTitle());
            orgAcctDetail.setAmount(pdcRecord.getAmount());
            orgAcctDetail.setTransactionCategory(TransactionCategory.CUSTOMER_PAYMENT);
            orgAcctDetail.setOrganizationAcctId(pdcRecord.getOrganizationAccountId());
            orgAcctDetail.setProjectId(pdcRecord.getProjectId() != null ? pdcRecord.getProjectId() : 0);

            organizationAccount = organizationAccountService.deductFromOrgAcct(orgAcctDetail, loggedInUser);

            // 6. Create expense detail record
            ExpenseDetail expenseDetail = new ExpenseDetail();
            expenseDetail.setExpenseId(expense.getId());
            expenseDetail.setAmountPaid(pdcRecord.getAmount());
            expenseDetail.setOrganizationAccountId(pdcRecord.getOrganizationAccountId());
            expenseDetail.setOrganizationAccountTitle(organizationAccount.getName());
            expenseDetail.setExpenseTitle(expense.getExpenseTitle());
            expenseDetail.setPaymentType(PaymentType.CHEQUE);
            expenseDetail.setPaymentDocNo(pdcRecord.getChequeNumber());
            expenseDetail.setPaymentDocDate(pdcRecord.getChequeDate() != null ? pdcRecord.getChequeDate().atStartOfDay() : null);
            expenseDetail.setCreatedBy(loggedInUser);
            expenseDetail.setUpdatedBy(loggedInUser);
            expenseDetailRepo.save(expenseDetail);

            // 7. Decrease vendor payable (was increased during PDC creation)
            if (vendorOpt.isPresent()) {
                VendorAccount vendorAccount = vendorOpt.get();
                vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + pdcRecord.getAmount());
                double updatedCreditBalance = vendorAccount.getTotalCreditAmount() - pdcRecord.getAmount();
                vendorAccount.setTotalCreditAmount(Math.max(updatedCreditBalance, 0));
                vendorAccount.setUpdatedBy(loggedInUser);
                vendorAccountRepo.save(vendorAccount);

                // Create vendor payment record
                VendorPayment vendorPayment = new VendorPayment();
                vendorPayment.setAmountPaid(pdcRecord.getAmount());
                vendorPayment.setOrganizationAccountId(pdcRecord.getOrganizationAccountId());
                vendorPayment.setCreditAmount(0);
                vendorPayment.setBalanceAmount(Math.max(updatedCreditBalance, 0));
                vendorPayment.setProjectId(pdcRecord.getProjectId() != null ? pdcRecord.getProjectId() : 0);
                vendorPayment.setTransactionType(TransactionType.DEBIT);
                vendorPayment.setVendorPaymentType(VendorPaymentType.PDC_CLEARANCE);
                vendorPayment.setVendorAccountId(pdcRecord.getVendorAccountId());
                vendorPayment.setExpenseId(expense.getId());
                vendorPayment.setComments("PDC Cleared - Cheque #" + pdcRecord.getChequeNumber());
                vendorPayment.setPaymentMethodType(PaymentType.CHEQUE);
                vendorPayment.setPaymentDocNo(pdcRecord.getChequeNumber());
                vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);
            }

            // 8. Update PDC record status and link to expense
            pdcRecord.setStatus(PdcStatus.CLEARED);
            pdcRecord.setExpenseId(expense.getId());
            pdcRecord.setUpdatedBy(loggedInUser);
            pdcRecordRepo.save(pdcRecord);

            // 9. Create journal entry for PDC clearance (Dr Expense/Payable, Cr Bank)
            journalEntryService.createJournalEntryForExpense(expense, organizationAccount, loggedInUser);

            log.info("PDC {} cleared successfully, expense {} created by {}", pdcId, expense.getId(), loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, Map.of(
                    "pdcRecord", pdcRecord,
                    "expense", expense
            ));

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error processing PDC {}: {}", pdcId, e.getMessage(), e);
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // ──────────────────────────────────────────────────────────────────────
    // 3. Mark PDC as Failed
    // POST /api/pdc/mark-failed/{id}
    // ──────────────────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> markFailed(long pdcId, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(pdcId, "pdc id");

            Optional<PdcRecord> pdcOpt = pdcRecordRepo.findById(pdcId);
            if (pdcOpt.isEmpty())
                throw new IllegalArgumentException("PDC record not found");

            PdcRecord pdcRecord = pdcOpt.get();

            if (pdcRecord.getStatus().equals(PdcStatus.CLEARED))
                throw new IllegalArgumentException("Cannot mark a cleared PDC as failed");

            if (pdcRecord.getStatus().equals(PdcStatus.FAILED))
                throw new IllegalArgumentException("This PDC is already marked as failed");

            // Mark as failed — no balance impact, vendor payable remains unchanged
            pdcRecord.setStatus(PdcStatus.FAILED);
            pdcRecord.setUpdatedBy(loggedInUser);
            pdcRecordRepo.save(pdcRecord);

            log.info("PDC {} marked as FAILED by {}", pdcId, loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, pdcRecord);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error marking PDC {} as failed: {}", pdcId, e.getMessage(), e);
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // ──────────────────────────────────────────────────────────────────────
    // 4. List PDC Payments with filters
    // POST /api/pdc/list
    // ──────────────────────────────────────────────────────────────────────
    public Map<String, Object> listPdcPayments(PdcListRequestDTO request, Pageable pageable) {
        try {
            ValidationService.validate(request.getOrganizationId(), "organization id");

            Long orgId = request.getOrganizationId();
            LocalDate today = LocalDate.now();
            Page<PdcRecord> result;

            String filter = request.getFilter() != null ? request.getFilter().toLowerCase() : "all";

            switch (filter) {
                case "duetoday":
                    result = pdcRecordRepo.findPdcDueToday(orgId, today, pageable);
                    break;

                case "overdue":
                    result = pdcRecordRepo.findPdcOverdue(orgId, today, pageable);
                    break;

                case "upcoming":
                    result = pdcRecordRepo.findPdcUpcoming(orgId, today, pageable);
                    break;

                case "all":
                default:
                    if (request.getVendorAccountId() != null && request.getVendorAccountId() > 0) {
                        result = pdcRecordRepo.findAllByOrganizationIdAndStatusAndVendorAccountId(
                                orgId, PdcStatus.PENDING, request.getVendorAccountId(), pageable);
                    } else if (request.getProjectId() != null && request.getProjectId() > 0) {
                        result = pdcRecordRepo.findAllByOrganizationIdAndStatusAndProjectId(
                                orgId, PdcStatus.PENDING, request.getProjectId(), pageable);
                    } else {
                        result = pdcRecordRepo.findAllByOrganizationIdAndStatus(
                                orgId, PdcStatus.PENDING, pageable);
                    }
                    break;
            }

            // Enrich with vendor and project names
            result.forEach(pdc -> {
                if (pdc.getVendorAccountId() != null) {
                    vendorAccountRepo.findById(pdc.getVendorAccountId())
                            .ifPresent(vendor -> pdc.setVendorName(vendor.getName()));
                }
                if (pdc.getProjectId() != null && pdc.getProjectId() > 0) {
                    projectRepo.findById(pdc.getProjectId())
                            .ifPresent(project -> pdc.setProjectName(project.getName()));
                }
                if (pdc.getOrganizationAccountId() != null) {
                    organizationAccountRepo.findById(pdc.getOrganizationAccountId())
                            .ifPresent(orgAcct -> pdc.setOrgAccountTitle(orgAcct.getName()));
                }
            });

            return ResponseMapper.buildResponse(Responses.SUCCESS, result);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            log.error("Error listing PDC payments: {}", e.getMessage(), e);
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // ──────────────────────────────────────────────────────────────────────
    // 5. Get PDC by ID
    // GET /api/pdc/{id}
    // ──────────────────────────────────────────────────────────────────────
    public Map<String, Object> getPdcById(long pdcId) {
        try {
            ValidationService.validate(pdcId, "pdc id");

            Optional<PdcRecord> pdcOpt = pdcRecordRepo.findById(pdcId);
            if (pdcOpt.isEmpty())
                throw new IllegalArgumentException("PDC record not found");

            PdcRecord pdcRecord = pdcOpt.get();

            // Enrich with vendor and project names
            if (pdcRecord.getVendorAccountId() != null) {
                vendorAccountRepo.findById(pdcRecord.getVendorAccountId())
                        .ifPresent(vendor -> pdcRecord.setVendorName(vendor.getName()));
            }
            if (pdcRecord.getProjectId() != null && pdcRecord.getProjectId() > 0) {
                projectRepo.findById(pdcRecord.getProjectId())
                        .ifPresent(project -> pdcRecord.setProjectName(project.getName()));
            }
            if (pdcRecord.getOrganizationAccountId() != null) {
                organizationAccountRepo.findById(pdcRecord.getOrganizationAccountId())
                        .ifPresent(orgAcct -> pdcRecord.setOrgAccountTitle(orgAcct.getName()));
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, pdcRecord);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching PDC {}: {}", pdcId, e.getMessage(), e);
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // ──────────────────────────────────────────────────────────────────────
    // 6. Partial Payment for PDC
    // POST /api/payments/pdc/partial-payment
    // ──────────────────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> makePartialPayment(Long pdcId, double paymentAmount, Long organizationAccountId,
                                                  PaymentType paymentType, String paymentDocNo, String comments, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(pdcId, "pdc id");
            ValidationService.validate(paymentAmount, "payment amount");
            ValidationService.validate(organizationAccountId, "organization account id");
            ValidationService.validate(paymentType, "payment type");

            if (paymentAmount <= 0)
                throw new IllegalArgumentException("Payment amount must be greater than 0");

            // 1. Fetch PDC record
            Optional<PdcRecord> pdcOpt = pdcRecordRepo.findById(pdcId);
            if (pdcOpt.isEmpty())
                throw new IllegalArgumentException("PDC record not found");

            PdcRecord pdcRecord = pdcOpt.get();

            // 2. Validate status
            if (pdcRecord.getStatus().equals(PdcStatus.CLEARED))
                throw new IllegalArgumentException("This PDC has already been fully cleared");

            if (pdcRecord.getStatus().equals(PdcStatus.FAILED))
                throw new IllegalArgumentException("Cannot make payment for a failed PDC");

            // 3. Check if payment amount exceeds remaining amount
            double remainingAmount = pdcRecord.getAmount() - pdcRecord.getPaidAmount();
            if (paymentAmount > remainingAmount)
                throw new IllegalArgumentException("Payment amount (" + paymentAmount
                        + ") exceeds remaining PDC amount (" + remainingAmount + ")");

            // 4. Validate organization account balance
            Optional<OrganizationAccount> orgAccountOpt = organizationAccountRepo.findById(organizationAccountId);
            if (orgAccountOpt.isEmpty())
                throw new IllegalArgumentException("Organization account not found");

            OrganizationAccount organizationAccount = orgAccountOpt.get();
            double accountBalance = organizationAccount.getTotalAmount() - paymentAmount;

            if (accountBalance < 0)
                throw new IllegalArgumentException("Insufficient balance in account: " + organizationAccount.getName()
                        + ". Available: " + organizationAccount.getTotalAmount()
                        + ", Required: " + paymentAmount);

            // 5. Create Expense for this partial payment
            Expense expense = new Expense();
            expense.setOrganizationId(pdcRecord.getOrganizationId());
            expense.setVendorAccountId(pdcRecord.getVendorAccountId());
            expense.setProjectId(pdcRecord.getProjectId());
            expense.setUnitId(pdcRecord.getUnitId());
            expense.setExpenseTypeId(pdcRecord.getExpenseTypeId());
            expense.setOrganizationAccountId(organizationAccountId);
            expense.setAmountPaid(paymentAmount);
            expense.setCreditAmount(0);
            expense.setTotalAmount(paymentAmount);
            expense.setPaymentStatus(PaymentStatus.PAID);
            expense.setPdcStatus(null); // Not directly from PDC clearance
            expense.setExpenseTitle(pdcRecord.getTitle() + " - Partial Payment");
            expense.setComments("Partial payment for PDC #" + pdcRecord.getId() + " Cheque #" + pdcRecord.getChequeNumber()
                    + (comments != null ? " | " + comments : ""));
            expense.setCreatedBy(loggedInUser);
            expense.setUpdatedBy(loggedInUser);

            // Get vendor and project names
            Optional<VendorAccount> vendorOpt = vendorAccountRepo.findById(pdcRecord.getVendorAccountId());
            if (vendorOpt.isPresent()) {
                expense.setVendorName(vendorOpt.get().getName());
            }

            if (pdcRecord.getProjectId() != null && pdcRecord.getProjectId() > 0) {
                Optional<Project> projectOpt = projectRepo.findById(pdcRecord.getProjectId());
                if (projectOpt.isPresent()) {
                    expense.setProjectName(projectOpt.get().getName());
                }
            }

            // Set expense COA ID
//            expense.setExpenseCOAId(journalEntryService.getConstructionInventoryControlAccount(pdcRecord.getOrganizationId()).getId());

            expense = expenseRepo.save(expense);
            expense.setOrgAccountTitle(organizationAccount.getName());

            // 6. Deduct from organization account
            OrganizationAccountDetail orgAcctDetail = new OrganizationAccountDetail();
            orgAcctDetail.setExpenseId(expense.getId());
            orgAcctDetail.setComments("Partial payment for PDC #" + pdcRecord.getId() + " Cheque #" + pdcRecord.getChequeNumber()
                    + " | " + expense.getExpenseTitle());
            orgAcctDetail.setAmount(paymentAmount);
            orgAcctDetail.setTransactionCategory(TransactionCategory.CUSTOMER_PAYMENT);
            orgAcctDetail.setOrganizationAcctId(organizationAccountId);
            orgAcctDetail.setProjectId(pdcRecord.getProjectId() != null ? pdcRecord.getProjectId() : 0);

            organizationAccount = organizationAccountService.deductFromOrgAcct(orgAcctDetail, loggedInUser);

            // 7. Create expense detail record
            ExpenseDetail expenseDetail = new ExpenseDetail();
            expenseDetail.setExpenseId(expense.getId());
            expenseDetail.setAmountPaid(paymentAmount);
            expenseDetail.setOrganizationAccountId(organizationAccountId);
            expenseDetail.setOrganizationAccountTitle(organizationAccount.getName());
            expenseDetail.setExpenseTitle(expense.getExpenseTitle());
            expenseDetail.setPaymentType(paymentType);
            expenseDetail.setPaymentDocNo(paymentDocNo);
            expenseDetail.setPaymentDocDate(LocalDateTime.now());
            expenseDetail.setCreatedBy(loggedInUser);
            expenseDetail.setUpdatedBy(loggedInUser);
            expenseDetailRepo.save(expenseDetail);

            if (vendorOpt.isPresent()) {
                VendorAccount vendorAccount = vendorOpt.get();
                vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + paymentAmount);
                double updatedCreditBalance = vendorAccount.getTotalCreditAmount() - paymentAmount;
                vendorAccount.setTotalCreditAmount(Math.max(updatedCreditBalance, 0));
                vendorAccount.setUpdatedBy(loggedInUser);
                vendorAccountRepo.save(vendorAccount);

                // Create vendor payment record
                VendorPayment vendorPayment = new VendorPayment();
                vendorPayment.setAmountPaid(paymentAmount);
                vendorPayment.setOrganizationAccountId(organizationAccountId);
                vendorPayment.setCreditAmount(0);
                vendorPayment.setBalanceAmount(Math.max(updatedCreditBalance, 0));
                vendorPayment.setProjectId(pdcRecord.getProjectId() != null ? pdcRecord.getProjectId() : 0);
                vendorPayment.setMaterialAmount(0);
                vendorPayment.setTransactionType(TransactionType.DEBIT);
                vendorPayment.setVendorPaymentType(VendorPaymentType.PDC_CLEARANCE);
                vendorPayment.setVendorAccountId(pdcRecord.getVendorAccountId());
                vendorPayment.setExpenseId(expense.getId());
                vendorPayment.setComments("Partial payment for PDC #" + pdcRecord.getId() + " Cheque #" + pdcRecord.getChequeNumber());
                vendorPayment.setPaymentMethodType(paymentType);
                vendorPayment.setPaymentDocNo(paymentDocNo);
                vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);
            }

            // 9. Update PDC record with paid amount
            pdcRecord.setPaidAmount(pdcRecord.getPaidAmount() + paymentAmount);
            pdcRecord.setUpdatedBy(loggedInUser);

            // If fully paid, mark as CLEARED
            if (pdcRecord.getPaidAmount() >= pdcRecord.getAmount()) {
                pdcRecord.setStatus(PdcStatus.CLEARED);
                pdcRecord.setExpenseId(expense.getId()); // Link last expense
                log.info("PDC {} fully paid via partial payments", pdcId);
            }

            pdcRecordRepo.save(pdcRecord);

            // 10. Create journal entry
            journalEntryService.createJournalEntryForExpense(expense, organizationAccount, loggedInUser);

            log.info("Partial payment of {} made for PDC {} by {}", paymentAmount, pdcId, loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, Map.of(
                    "pdcRecord", pdcRecord,
                    "expense", expense,
                    "paidAmount", pdcRecord.getPaidAmount(),
                    "remainingAmount", pdcRecord.getAmount() - pdcRecord.getPaidAmount(),
                    "status", pdcRecord.getStatus()
            ));

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error making partial payment for PDC {}: {}", pdcId, e.getMessage(), e);
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
