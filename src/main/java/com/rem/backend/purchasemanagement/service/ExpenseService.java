package com.rem.backend.purchasemanagement.service;

import com.rem.backend.accountingmanagement.repos.AccountGroupRepository;
import com.rem.backend.accountingmanagement.repos.ChartOfAccountRepository;
import com.rem.backend.accountingmanagement.utility.JournalUtilities;
import com.rem.backend.organizationmanagement.enums.TransactionCategory;
import com.rem.backend.organizationmanagement.service.OrganizationAccountService;
import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.dto.expense.ExpenseFetchRequestDTO;
import com.rem.backend.accountingmanagement.entity.ChartOfAccount;
import com.rem.backend.purchasemanagement.entity.expense.ExpenseDetail;
import com.rem.backend.purchasemanagement.entity.expense.ExpenseType;
import com.rem.backend.purchasemanagement.entity.expense.Expense;
import com.rem.backend.vendormanagement.entity.PdcRecord;
import com.rem.backend.organizationmanagement.entity.OrganizationAccount;
import com.rem.backend.organizationmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.projectmanagement.entity.Project;
import com.rem.backend.projectmanagement.entity.Unit;
import com.rem.backend.vendormanagement.entity.VendorAccount;
import com.rem.backend.vendormanagement.entity.VendorPayment;
import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.enums.PaymentType;
import com.rem.backend.enums.TransactionType;
import com.rem.backend.enums.VendorPaymentType;
import com.rem.backend.purchasemanagement.repository.ExpenseDetailRepo;
import com.rem.backend.purchasemanagement.repository.ExpenseRepo;
import com.rem.backend.purchasemanagement.repository.ExpenseTypeRepo;
import com.rem.backend.organizationmanagement.repository.OrganizationAccoutRepo;
import com.rem.backend.organizationmanagement.repository.OrganizationAccountDetailRepo;
import com.rem.backend.projectmanagement.repository.ProjectRepo;
import com.rem.backend.projectmanagement.repository.UnitRepo;
import com.rem.backend.vendormanagement.repository.VendorAccountDetailRepo;
import com.rem.backend.vendormanagement.repository.VendorAccountRepo;
import com.rem.backend.accountingmanagement.service.JournalEntryService;
import com.rem.backend.vendormanagement.service.PdcPaymentService;
import com.rem.backend.vendormanagement.service.VendorAccountService;
import com.rem.backend.warehousemanagement.service.WarehouseIntegrationService;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.Utility;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.rem.backend.utility.Utility.getPaymentStatus;

@Service
@AllArgsConstructor
public class ExpenseService {

    private final ExpenseRepo expenseRepo;
    private final ExpenseTypeRepo expenseTypeRepo;
    private final OrganizationAccountService organizationAccountService;
    private final VendorAccountRepo vendorAccountRepo;
    private final ProjectRepo projectRepo;
    private final ExpenseDetailRepo expenseDetailRepo;
    private final OrganizationAccoutRepo organizationAccoutRepo;
    private final OrganizationAccountDetailRepo organizationAccountDetailRepo;
    private final VendorAccountService vendorAccountService;
    private final VendorAccountDetailRepo vendorAccountDetailRepo;
    private final OrganizationAccoutRepo organizationAccountRepo;
    private final JournalEntryService journalEntryService;
    private final JournalUtilities journalUtilities;
    private final AccountGroupRepository accountGroupRepository;
    private final ChartOfAccountRepository coaRepo;
    private final WarehouseIntegrationService warehouseIntegrationService;
    private final PdcPaymentService pdcPaymentService;
    private final UnitRepo unitRepo;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getExpenseList(ExpenseFetchRequestDTO requestDTO, Pageable pageable) {

        try {

            Page<Expense> expenses = null;

            // ===========================
            // 1️⃣ Validations
            // ===========================
            ValidationService.validate(requestDTO.getId(), "Invalid " + requestDTO.getFilteredBy());
            ValidationService.validate(requestDTO.getExpenseType(), "Invalid Expense Type");


            LocalDateTime startDate;
            LocalDateTime endDate;

            if (requestDTO.getStartDate() == null || requestDTO.getEndDate() == null) {
                // Default to 10 years
                startDate = Utility.getStartOfDay(requestDTO.getStartDate());
                endDate = Utility.getEndOfDay(requestDTO.getEndDate());
            } else {
                startDate = Utility.getStartOfDay(requestDTO.getStartDate());
                endDate = Utility.getEndOfDay(requestDTO.getEndDate());
            }

            // ===========================
            // 2️⃣ CONSTRUCTION Expenses
            // ===========================
            if (requestDTO.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {

                ValidationService.validate(requestDTO.getPaymentStatus(), "Payment Status");

                switch (requestDTO.getFilteredBy()) {

                    case "vendor":
                        if (requestDTO.getPaymentStatus().equals(PaymentStatus.ALL)) {
                            expenses = expenseRepo.
                                    findAllByVendorAccountIdAndExpenseTypeAndCreatedDateBetween(
                                            requestDTO.getId(),
                                            com.rem.backend.enums.ExpenseType.CONSTRUCTION,
                                            startDate,
                                            endDate,
                                            pageable
                                    );
                        } else {
                            expenses = expenseRepo
                                    .findAllByVendorAccountIdAndPaymentStatusAndExpenseTypeAndCreatedDateBetween(
                                            requestDTO.getId(),
                                            requestDTO.getPaymentStatus(),
                                            com.rem.backend.enums.ExpenseType.CONSTRUCTION,
                                            startDate,
                                            endDate,
                                            pageable
                                    );
                        }
                        break;

                    case "project":
                        if (requestDTO.getPaymentStatus().equals(PaymentStatus.ALL)) {
                            expenses = expenseRepo
                                    .findAllByProjectIdAndExpenseTypeAndCreatedDateBetween(
                                            requestDTO.getId(),
                                            com.rem.backend.enums.ExpenseType.CONSTRUCTION,
                                            startDate,
                                            endDate,
                                            pageable
                                    );
                        } else {
                            expenses = expenseRepo
                                    .findAllByProjectIdAndPaymentStatusAndExpenseTypeAndCreatedDateBetween(
                                            requestDTO.getId(),
                                            requestDTO.getPaymentStatus(),
                                            com.rem.backend.enums.ExpenseType.CONSTRUCTION,
                                            startDate,
                                            endDate,
                                            pageable
                                    );
                        }

                        break;

                    case "project_vendor":


                        if (requestDTO.getPaymentStatus().equals(PaymentStatus.ALL)) {
                            expenses = expenseRepo.findAllByProjectIdAndVendorAccountIdAndExpenseTypeAndCreatedDateBetween(
                                    requestDTO.getId(),
                                    requestDTO.getId2(),
                                    com.rem.backend.enums.ExpenseType.CONSTRUCTION,
                                    startDate,
                                    endDate,
                                    pageable
                            );
                        } else {
                            expenses = expenseRepo
                                    .findAllByProjectIdAndPaymentStatusAndVendorAccountIdAndExpenseTypeAndCreatedDateBetween(
                                            requestDTO.getId(),
                                            requestDTO.getPaymentStatus(),
                                            requestDTO.getId2(),
                                            com.rem.backend.enums.ExpenseType.CONSTRUCTION,
                                            startDate,
                                            endDate,
                                            pageable
                                    );
                        }
                        break;

                    default:

                        if (requestDTO.getPaymentStatus().equals(PaymentStatus.ALL)) {
                            expenses = expenseRepo.findAllByOrganizationIdAndExpenseTypeAndCreatedDateBetween(
                                    requestDTO.getId(),
                                    com.rem.backend.enums.ExpenseType.CONSTRUCTION,
                                    startDate,
                                    endDate,
                                    pageable
                            );
                        } else {
                            expenses = expenseRepo
                                    .findAllByOrganizationIdAndPaymentStatusAndExpenseTypeAndCreatedDateBetween(
                                            requestDTO.getId(),
                                            requestDTO.getPaymentStatus(),
                                            com.rem.backend.enums.ExpenseType.CONSTRUCTION,
                                            startDate,
                                            endDate,
                                            pageable
                                    );

                        }
                }



                Set<Long> expenseCoaIds = expenses.getContent().stream()
                        .map(Expense::getExpenseCOAId)
                        .filter(id -> id != null && id != 0)
                        .collect(Collectors.toSet());

                Map<Long, ChartOfAccount> coaMap = coaRepo.findAllById(expenseCoaIds)
                        .stream()
                        .collect(Collectors.toMap(ChartOfAccount::getId, Function.identity()));

                expenses = expenses.map(expense -> {

                    ChartOfAccount coa = coaMap.get(expense.getExpenseCOAId());
                    if (coa != null) {
                        expense.setExpenseAccountName(
                                coa.getAccountGroup().getName() + " - " + coa.getName()
                        );
                    }

                    return expense;
                });

//                expenses = expenses.map(expense -> {
//                    expense.setExpenseAccountName("Construction Inventory");
//                    return expense;
//                });
            } else if (requestDTO.getExpenseType().equals(com.rem.backend.enums.ExpenseType.MISCELLANEOUS)) {

                // Case 1: Account Group + COA selected
                if (requestDTO.getAccountGroupId() != null && requestDTO.getCoaId() != null) {

                    expenses = expenseRepo
                            .findByExpenseCOAIdAndOrganizationIdAndCreatedDateBetween(
                                    requestDTO.getCoaId(),
                                    requestDTO.getId(),
                                    startDate,
                                    endDate,
                                    pageable
                            );
                }

                // Case 2: Account Group selected but COA not selected
                else if (requestDTO.getAccountGroupId() != null) {

                    List<ChartOfAccount> chartOfAccounts =
                            coaRepo.findAllByOrganization_OrganizationIdAndAccountGroup_Id(
                                    requestDTO.getId(),
                                    requestDTO.getAccountGroupId()
                            );

                    List<Long> coaIds = chartOfAccounts.stream()
                            .map(ChartOfAccount::getId)
                            .toList();

                    expenses = expenseRepo
                            .findByOrganizationIdAndExpenseCOAIdInAndCreatedDateBetween(
                                    requestDTO.getId(),
                                    coaIds,
                                    startDate,
                                    endDate,
                                    pageable
                            );
                }

                // Case 3: No Account Group selected (All Misc Expenses)
                else {

                    expenses = expenseRepo
                            .findByOrgAndCoaOrTitleWithinDate(
                                    requestDTO.getId(),
                                    startDate,
                                    endDate,
                                    "Miscellaneous Expense",
                                    com.rem.backend.enums.ExpenseType.MISCELLANEOUS,
                                    pageable
                            );
                }

                Set<Long> expenseCoaIds = expenses.getContent().stream()
                        .map(Expense::getExpenseCOAId)
                        .filter(id -> id != null && id != 0)
                        .collect(Collectors.toSet());

                Map<Long, ChartOfAccount> coaMap = coaRepo.findAllById(expenseCoaIds)
                        .stream()
                        .collect(Collectors.toMap(ChartOfAccount::getId, Function.identity()));

                expenses = expenses.map(expense -> {

                    ChartOfAccount coa = coaMap.get(expense.getExpenseCOAId());
                    if (coa != null) {
                        expense.setExpenseAccountName(
                                coa.getAccountGroup().getName() + " - " + coa.getName()
                        );
                    }

                    return expense;
                });

            } else if (requestDTO.getExpenseType().equals(com.rem.backend.enums.ExpenseType.ALL)) {

                expenses = expenseRepo
                        .findAllByOrganizationIdAndCreatedDateBetween(
                                requestDTO.getId(),
                                startDate,
                                endDate,
                                pageable
                        );

                Set<Long> expenseCoaIds = expenses.getContent().stream()
                        .map(Expense::getExpenseCOAId)
                        .filter(id -> id != null && id != 0)
                        .collect(Collectors.toSet());

                Map<Long, ChartOfAccount> coaMap = coaRepo.findAllById(expenseCoaIds)
                        .stream()
                        .collect(Collectors.toMap(ChartOfAccount::getId, Function.identity()));

                expenses = expenses.map(expense -> {

                    if (expense.getExpenseCOAId() != 0) {
                        ChartOfAccount coa = coaMap.get(expense.getExpenseCOAId());
                        if (coa != null) {
                            expense.setExpenseAccountName(
                                    coa.getAccountGroup().getName() + " - " + coa.getName()
                            );
                        }
                    } else if(expense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {
                        expense.setExpenseAccountName("Construction Inventory");
                    }


                    return expense;
                });
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, populateLegacyAccountGroups(expenses));

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getPayableExpenseList(FilterPaginationRequest request, Pageable pageable) {

        Page<Expense> expenses = null;
        try {

            ValidationService.validate(request.getId(), "organization");


            if (request.getId2() == 0) {
                expenses = expenseRepo.
                        findAllByOrganizationIdAndCreditAmountGreaterThan(request.getId(), 0.0, pageable);
            } else {

                expenses = expenseRepo.
                        findAllByOrganizationIdAndVendorAccountIdAndCreditAmountGreaterThan(request.getId(),
                                request.getId2(), 0.0, pageable);
            }


            return ResponseMapper.buildResponse(Responses.SUCCESS, populateLegacyAccountGroups(expenses));

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getExpenseDetails(long expenseId) {

        try {

            List<ExpenseDetail> expenseDetails = new ArrayList<>();
            ValidationService.validate(expenseId, "expenseId");


            Optional<Expense> expenseOptional = expenseRepo.findById(expenseId);

            if (expenseOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Expense");

            Expense expense = expenseOptional.get();
            populateLegacyAccountGroups(List.of(expense));
            expenseDetails = expenseDetailRepo.findByExpenseIdOrderByCreatedDateDesc(expenseId);

            Map<String, Object> data = new HashMap<>();
            data.put("expense", expense);
            data.put("expenseDetail", expenseDetails);


            return ResponseMapper.buildResponse(Responses.SUCCESS, data);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    @Transactional
    public Map<String, Object> addExpense(Expense expense, String loggedInUser) {

        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(expense.getExpenseType(), "expense type");
            ValidationService.validate(expense.getAmountPaid(), "amount paid");
            ValidationService.validate(expense.getTotalAmount(), "total amount");
            ValidationService.validate(expense.getOrganizationId(), "organization id");
            ValidationService.validate(expense.getPaymentType(), "payment type");
            ValidationService.validate(expense.getOrganizationAccountId(), "organization account id");

            if (expense.getExpenseType() == com.rem.backend.enums.ExpenseType.MISCELLANEOUS) {
                if (expense.getPaymentType() == PaymentType.CHEQUE) {
                    throw new IllegalArgumentException("Post-dated cheques are not supported for miscellaneous expenses");
                }
                if (!Double.isFinite(expense.getTotalAmount())
                        || !Double.isFinite(expense.getAmountPaid())
                        || !Double.isFinite(expense.getCreditAmount())
                        || expense.getTotalAmount() <= 0 || expense.getAmountPaid() < 0
                        || expense.getCreditAmount() < 0
                        || Math.abs(expense.getAmountPaid() + expense.getCreditAmount()
                        - expense.getTotalAmount()) > 0.01) {
                    throw new IllegalArgumentException(
                            "Total amount must equal amount paid plus credit amount, and all amounts must be valid");
                }
                setExpenseAccountGroup(expense, loggedInUser);
                expense.setProjectId(0L);
                expense.setProjectName(null);
                expense.setUnitId(null);
                validateMiscellaneousVendor(expense);
            } else {
                expense.setAccountGroupId(null);
            }

            // ── PDC MODE: determine if this is a Post-Dated Cheque ──────────
            // PDC flow is triggered when paymentType == CHEQUE

            if (expense.getProjectId() != null && expense.getProjectId() > 0l) {
                Optional<Project> projectOptional = projectRepo.findById(expense.getProjectId());
                if (!projectOptional.isPresent())
                    throw new IllegalArgumentException("Invalid Project!");


                Project project = projectOptional.get();
                expense.setProjectName(project.getName());
                project.setConstructionAmount(project.getConstructionAmount() + expense.getTotalAmount());
                project.setTotalAmount(project.getTotalAmount() + expense.getTotalAmount());
                project.setUpdatedBy(loggedInUser);
                projectRepo.save(project);
            }
            boolean isPdc = expense.getPaymentType() != null && expense.getPaymentType().equals(PaymentType.CHEQUE);

            if (isPdc) {
                // DELEGATE TO PDC SERVICE - DO NOT CREATE EXPENSE
                // PDC validations
                if (expense.getChequeNumber() == null || expense.getChequeNumber().isBlank())
                    throw new IllegalArgumentException("Cheque number is required for PDC payments");
                if (expense.getChequeDate() == null)
                    throw new IllegalArgumentException("Cheque date is required for PDC payments");
                if (expense.getChequeDate().isBefore(java.time.LocalDate.now()))
                    throw new IllegalArgumentException("Cheque date must be today or a future date");
                if (expense.getAmountPaid() <= 0 && expense.getTotalAmount() <= 0)
                    throw new IllegalArgumentException("Amount must be greater than 0 for PDC payments");

                // Convert Expense to PdcRecord
                PdcRecord pdcRecord = new PdcRecord();
                pdcRecord.setOrganizationId(expense.getOrganizationId());
                pdcRecord.setVendorAccountId(expense.getVendorAccountId());
                pdcRecord.setProjectId(expense.getProjectId());
                pdcRecord.setUnitId(expense.getUnitId());
                pdcRecord.setExpenseTypeId(expense.getExpenseTypeId());
                pdcRecord.setOrganizationAccountId(expense.getOrganizationAccountId());
                pdcRecord.setAmount(expense.getTotalAmount()); // Use total amount for PDC
                pdcRecord.setChequeNumber(expense.getChequeNumber());
                pdcRecord.setChequeDate(expense.getChequeDate());
                pdcRecord.setBankName(expense.getBankName());
                pdcRecord.setTitle(expense.getExpenseTitle() != null ? expense.getExpenseTitle() : "PDC Payment");
                pdcRecord.setComments(expense.getComments());


                Optional<OrganizationAccount> organizationAccountOptional = organizationAccountRepo
                        .findByIdAndOrganizationId(expense.getOrganizationAccountId(), expense.getOrganizationId());
                if (!organizationAccountOptional.isPresent())
                    throw new IllegalArgumentException("Invalid Account");
//
                // Create journal entry for expense (double-entry bookkeeping)
                journalEntryService.createJournalEntryForExpense(expense, organizationAccountOptional.get(), loggedInUser);

                // Delegate to PdcPaymentService
                return pdcPaymentService.createPdcRecord(pdcRecord, loggedInUser);
            }

            // ── CASH / CREDIT: existing flow ────────────────────────────

            OrganizationAccountDetail organizationAccountDetail = new OrganizationAccountDetail();

            Optional<OrganizationAccount> organizationAccountOptional = organizationAccountRepo
                    .findByIdAndOrganizationId(expense.getOrganizationAccountId(), expense.getOrganizationId());

            if (!organizationAccountOptional.isPresent())
                throw new IllegalArgumentException("Invalid Account");

            // Balance validation for non-PDC payments
            OrganizationAccount organizationAccountValidate = organizationAccountOptional.get();
            double remainingAmount = organizationAccountValidate.getTotalAmount() - expense.getAmountPaid();
            if (remainingAmount < 0)
                throw new IllegalArgumentException("Not Enough Funds for this account");

            double updatedCreditBalance = 0;
            if (expense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {

                // Project is optional for CONSTRUCTION expenses (e.g., for purchase orders)
                ValidationService.validate(expense.getVendorAccountId(), "Vendor");
                ValidationService.validate(expense.getOrganizationAccountId(), "Organization Account");

                Optional<VendorAccount> accountOptional = vendorAccountRepo.findById(expense.getVendorAccountId());
                if (!accountOptional.isPresent())
                    throw new IllegalArgumentException("Invalid Vendor");

                VendorAccount vendorAccount = accountOptional.get();
                vendorAccount.setTotalAmount(vendorAccount.getTotalAmount() + expense.getTotalAmount());
                vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + expense.getAmountPaid());
                updatedCreditBalance = vendorAccount.getTotalCreditAmount() + expense.getCreditAmount();
                vendorAccount.setTotalCreditAmount(updatedCreditBalance);
                vendorAccountRepo.save(vendorAccount);


                expense.setVendorName(accountOptional.get().getName());
                organizationAccountDetail.setProjectId(expense.getProjectId() != null ? expense.getProjectId() : 0L);

                expense.setExpenseCOAId(journalUtilities.constructionInventory(expense.getOrganizationId()).getId());

            }

            expense.setUpdatedBy(loggedInUser);
            expense.setCreatedBy(loggedInUser);
            expense.setPaymentStatus(getPaymentStatus(expense));
            expense = expenseRepo.save(expense);

            if (expense.getExpenseType() == com.rem.backend.enums.ExpenseType.MISCELLANEOUS) {
                addMiscellaneousVendorPayment(expense, loggedInUser);
            }
            populateExpenseAccountDetail(organizationAccountDetail, expense);
            OrganizationAccount organizationAccount = organizationAccountService.deductFromOrgAcct(organizationAccountDetail, loggedInUser);
            expense.setOrgAccountTitle(organizationAccount.getName());


            ExpenseDetail expenseDetail = new ExpenseDetail();
            expenseDetail.setExpenseId(expense.getId());
            expenseDetail.setAmountPaid(expense.getAmountPaid());
            expenseDetail.setOrganizationAccountId(expense.getOrganizationAccountId());
            expenseDetail.setOrganizationAccountTitle(organizationAccount.getName());
            expenseDetail.setExpenseTitle(expense.getExpenseTitle());
            expenseDetail.setPaymentType(expense.getPaymentType());
            expenseDetail.setPaymentDocNo(expense.getPaymentDocNo());
            expenseDetail.setPaymentDocDate(expense.getPaymentDocDate());

            if (!expenseDetail.getPaymentType().equals(PaymentType.CHEQUE) &&
                    !expenseDetail.getPaymentType().equals(PaymentType.PAY_ORDER)) {
                expenseDetail.setPaymentDocDate(null);
                expenseDetail.setPaymentDocNo(null);
            }

            expenseDetail.setExpenseTitle(expense.getExpenseTitle());
            expenseDetail.setUpdatedBy(loggedInUser);
            expenseDetail.setCreatedBy(loggedInUser);
            expenseDetailRepo.save(expenseDetail);


            if (expense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {
                VendorPayment vendorPayment = new VendorPayment();
                vendorPayment.setAmountPaid(expense.getAmountPaid());
                vendorPayment.setOrganizationAccountId(expense.getOrganizationAccountId());
                vendorPayment.setCreditAmount(expense.getCreditAmount());
                vendorPayment.setBalanceAmount(updatedCreditBalance);
                vendorPayment.setProjectId(expense.getProjectId() != null ? expense.getProjectId() : 0L);
                vendorPayment.setVendorPaymentType(VendorPaymentType.DIRECT_PURCHASE);
                if (expense.getCreditAmount() == 0) {
                    vendorPayment.setTransactionType(TransactionType.DEBIT);
                } else if (expense.getAmountPaid() == 0) {
                    vendorPayment.setTransactionType(TransactionType.CREDIT);
                } else {
                    vendorPayment.setTransactionType(TransactionType.DEBIT_CREDIT);
                }
                vendorPayment.setVendorAccountId(expense.getVendorAccountId());
                vendorPayment.setVendorAccountId(expense.getVendorAccountId());
                vendorPayment.setExpenseId(expense.getId());
                vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);
            }

            // Create journal entry for expense (double-entry bookkeeping)
            journalEntryService.createJournalEntryForExpense(expense, organizationAccount, loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, expense);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    @Transactional
    public Map<String, Object> updateExpense(Expense newExpense, String loggedInUser) {

        try {
            ValidationService.validate(newExpense.getId(), "Expense");
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(newExpense.getExpenseType(), "newExpense type");
            ValidationService.validate(newExpense.getAmountPaid(), "amount paid");
            ValidationService.validate(newExpense.getTotalAmount(), "total amount");
            ValidationService.validate(newExpense.getOrganizationId(), "organization id");
            ValidationService.validate(newExpense.getOrganizationAccountId(), "organization account id");

            boolean isExpenseTypeChanges = false;

            Optional<Expense> expenseOptional = expenseRepo.findById(newExpense.getId());
            if (expenseOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Expense!");

            Expense oldExpense = expenseOptional.get();

            if (oldExpense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.HISTORICAL))
                throw new IllegalArgumentException("Historical expense cannot be edited!");
            validateExpenseCanBeModified(oldExpense);
            if (!Objects.equals(newExpense.getOrganizationId(), oldExpense.getOrganizationId())) {
                throw new IllegalArgumentException("Expense organization cannot be changed");
            }
            if (newExpense.getExpenseType() != com.rem.backend.enums.ExpenseType.CONSTRUCTION
                    && newExpense.getExpenseType() != com.rem.backend.enums.ExpenseType.MISCELLANEOUS) {
                throw new IllegalArgumentException("Only construction and miscellaneous expenses can be edited");
            }
            List<ExpenseDetail> oldDetails = expenseDetailRepo.findByExpenseIdOrderByCreatedDateDesc(oldExpense.getId());
            if (oldDetails.size() > 1) {
                throw new IllegalArgumentException("Expenses with multiple payments cannot be edited");
            }
            if (!Double.isFinite(newExpense.getTotalAmount())
                    || !Double.isFinite(newExpense.getAmountPaid())
                    || !Double.isFinite(newExpense.getCreditAmount())
                    || newExpense.getTotalAmount() <= 0
                    || newExpense.getAmountPaid() < 0
                    || newExpense.getCreditAmount() < 0
                    || Math.abs(newExpense.getAmountPaid() + newExpense.getCreditAmount()
                    - newExpense.getTotalAmount()) > 0.01) {
                throw new IllegalArgumentException(
                        "Total amount must equal amount paid plus credit amount, and all amounts must be valid");
            }

            if (newExpense.getExpenseType() == com.rem.backend.enums.ExpenseType.MISCELLANEOUS) {
                setExpenseAccountGroup(newExpense, loggedInUser);
                newExpense.setProjectId(0L);
                newExpense.setProjectName(null);
                newExpense.setUnitId(null);
                if ((newExpense.getVendorAccountId() == null || newExpense.getVendorAccountId() <= 0)
                        && oldExpense.getVendorAccountId() != null && oldExpense.getVendorAccountId() > 0) {
                    newExpense.setVendorAccountId(oldExpense.getVendorAccountId());
                }
                validateMiscellaneousVendor(newExpense);
            } else {
                newExpense.setAccountGroupId(null);
                newExpense.setExpenseCOAId(journalUtilities.constructionInventory(newExpense.getOrganizationId()).getId());
            }

            newExpense.setCreatedBy(oldExpense.getCreatedBy());
            newExpense.setCreatedDate(oldExpense.getCreatedDate());
            newExpense.setUpdatedBy(loggedInUser);
            newExpense.setPaymentStatus(getPaymentStatus(newExpense));
            OrganizationAccount updatedOrganizationAccount = organizationAccoutRepo
                    .findByIdAndOrganizationId(
                            newExpense.getOrganizationAccountId(), newExpense.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Organization Account!"));
            newExpense.setOrgAccountTitle(updatedOrganizationAccount.getName());


            if (!newExpense.getExpenseType().equals(oldExpense.getExpenseType())) {
                isExpenseTypeChanges = true;
                if (newExpense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.MISCELLANEOUS)) {

                    Optional<VendorAccount> vendorAccountOptional = vendorAccountRepo.findById(oldExpense.getVendorAccountId());
                    if (vendorAccountOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Vendor Account!");

                    Optional<VendorPayment> vendorPaymentOptional = vendorAccountDetailRepo.findByExpenseId(oldExpense.getId());
                    if (vendorPaymentOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Vendor Account Detail!");

                    VendorAccount vendorAccount = vendorAccountOptional.get();
                    VendorPayment vendorPayment = vendorPaymentOptional.get();

                    vendorAccount.setTotalAmount(vendorAccount.getTotalAmount() - (vendorPayment.getAmountPaid() + vendorPayment.getCreditAmount()));
                    vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() - vendorPayment.getAmountPaid());
                    vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() - vendorPayment.getCreditAmount());
                    vendorAccount.setUpdatedBy(loggedInUser);
                    vendorAccountRepo.save(vendorAccount);

                    vendorAccountDetailRepo.delete(vendorPayment);

                    newExpense.setProjectId(0l);
                    newExpense.setProjectName("");
                    if (newExpense.getExpenseTitle() == null || newExpense.getExpenseTitle().isBlank()) {
                        newExpense.setExpenseTitle("Miscellaneous Expense");
                    }

                    // Only update project if old expense had a project
                    if (oldExpense.getProjectId() != null && oldExpense.getProjectId() > 0) {
                        Project oldProject = projectRepo.findById(oldExpense.getProjectId())
                                .orElseThrow(() -> new IllegalArgumentException("Invalid old Project!"));

                        oldProject.setTotalAmount(oldProject.getTotalAmount() - oldExpense.getTotalAmount());
                        oldProject.setConstructionAmount(oldProject.getConstructionAmount() - oldExpense.getTotalAmount());

                        projectRepo.save(oldProject);
                    }

                } else if (newExpense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {

                    removeMiscellaneousVendorPayment(oldExpense, loggedInUser);
                    // Project is optional when changing to CONSTRUCTION type
                    Optional<Project> newProjectOptional = Optional.empty();
                    if (newExpense.getProjectId() != null && newExpense.getProjectId() > 0) {
                        newProjectOptional = projectRepo.findById(newExpense.getProjectId());
                        if (newProjectOptional.isEmpty())
                            throw new IllegalArgumentException("Invalid Project!");
                    }


                    Optional<VendorAccount> vendorAccountOptional = vendorAccountRepo.findById(newExpense.getVendorAccountId());
                    if (vendorAccountOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Vendor Account!");

                    Optional<ExpenseType> expenseTypeOptional = expenseTypeRepo.findById(newExpense.getExpenseTypeId());
                    if (!expenseTypeOptional.isPresent())
                        throw new IllegalArgumentException("Invalid Expense Type");

                    VendorAccount vendorAccount = vendorAccountOptional.get();


//                    TODO :: ADD NEW ROWS FOR VENDOR ACCOUNT AND DETAIL

                    vendorAccount.setTotalAmount(vendorAccount.getTotalAmount() + newExpense.getTotalAmount());
                    vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + newExpense.getAmountPaid());
                    vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() + newExpense.getCreditAmount());
                    vendorAccountRepo.save(vendorAccount);


                    VendorPayment vendorPayment = new VendorPayment();
                    vendorPayment.setAmountPaid(newExpense.getAmountPaid());
                    vendorPayment.setOrganizationAccountId(newExpense.getOrganizationAccountId());
                    vendorPayment.setCreditAmount(newExpense.getCreditAmount());
                    vendorPayment.setProjectId(newExpense.getProjectId() != null ? newExpense.getProjectId() : 0L);
                    if (newExpense.getCreditAmount() == 0) {
                        vendorPayment.setTransactionType(TransactionType.DEBIT);
                    } else if (newExpense.getAmountPaid() == 0) {
                        vendorPayment.setTransactionType(TransactionType.CREDIT);
                    } else {
                        vendorPayment.setTransactionType(TransactionType.DEBIT_CREDIT);
                    }
                    vendorPayment.setVendorAccountId(newExpense.getVendorAccountId());
                    vendorPayment.setVendorAccountId(newExpense.getVendorAccountId());
                    vendorPayment.setExpenseId(newExpense.getId());
                    vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);


                    newExpense.setVendorName(vendorAccount.getName());

                    // Update project amounts only if project is provided
                    if (newProjectOptional.isPresent()) {
                        Project newProject = newProjectOptional.get();

                        newExpense.setProjectName(newProject.getName());
                        newExpense.setExpenseTitle(expenseTypeOptional.get().getName());

                        double updatedConstructionAmount = newProject.getConstructionAmount() + newExpense.getTotalAmount();
                        double updatedTotalAmount = newProject.getTotalAmount() + newExpense.getTotalAmount();

                        newProject.setTotalAmount(updatedTotalAmount);
                        newProject.setConstructionAmount(updatedConstructionAmount);
                        newProject.setUpdatedBy(loggedInUser);

                        projectRepo.save(newProject);
                    } else {
                        newExpense.setExpenseTitle(expenseTypeOptional.get().getName());
                    }

                }
            }

            expenseDetailRepo.deleteByExpenseId(oldExpense.getId());

            if (newExpense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {

                // Project is optional for CONSTRUCTION expenses (e.g., for purchase orders)
                // ValidationService.validate(newExpense.getProjectId(), "Project"); // Removed - project is now optional
                ValidationService.validate(newExpense.getVendorAccountId(), "Vendor");

                if (!Objects.equals(newExpense.getOrganizationAccountId(), oldExpense.getOrganizationAccountId())) {

                    Optional<OrganizationAccount> organizationAccountOptional = organizationAccoutRepo.findById(oldExpense.getOrganizationAccountId());
                    if (organizationAccountOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Organization Account!");


                    Optional<OrganizationAccountDetail> organizationAccountDetailOptional = organizationAccountDetailRepo.findByExpenseId(oldExpense.getId());
                    if (organizationAccountDetailOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Organization Account Detail!");

                    OrganizationAccount organizationAccount = organizationAccountOptional.get();
                    OrganizationAccountDetail organizationAccountDetail = organizationAccountDetailOptional.get();

                    organizationAccount.setTotalAmount(organizationAccount.getTotalAmount() + organizationAccountDetail.getAmount());
                    organizationAccount.setUpdatedBy(loggedInUser);
                    organizationAccoutRepo.save(organizationAccount);

                    organizationAccountDetailRepo.delete(organizationAccountDetail);

                    organizationAccountDetail = new OrganizationAccountDetail();

                    populateExpenseAccountDetail(organizationAccountDetail, newExpense);

                    organizationAccount = organizationAccountService.deductFromOrgAcct(organizationAccountDetail, loggedInUser);

                    newExpense.setOrgAccountTitle(organizationAccount.getName());

                } else {


                    Optional<OrganizationAccount> organizationAccountOptional = organizationAccoutRepo.findById(newExpense.getOrganizationAccountId());
                    if (organizationAccountOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Organization Account!");


                    Optional<OrganizationAccountDetail> organizationAccountDetailOptional = organizationAccountDetailRepo.findByExpenseId(newExpense.getId());
                    if (organizationAccountDetailOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Organization Account Detail!");

                    OrganizationAccount organizationAccount = organizationAccountOptional.get();
                    OrganizationAccountDetail organizationAccountDetail = organizationAccountDetailOptional.get();

                    double latestAmountOrg = organizationAccount.getTotalAmount() + (oldExpense.getAmountPaid() - newExpense.getAmountPaid());
                    if (latestAmountOrg < 0) {
                        throw new IllegalArgumentException("Not Enough Funds for this account");
                    }
                    organizationAccount.setTotalAmount(latestAmountOrg);
                    organizationAccount.setUpdatedBy(loggedInUser);
                    organizationAccoutRepo.save(organizationAccount);

                    populateExpenseAccountDetail(organizationAccountDetail, newExpense);
                    organizationAccountDetail.setAmount(newExpense.getAmountPaid());
                    organizationAccountDetail.setUpdatedBy(loggedInUser);
                    organizationAccountDetailRepo.save(organizationAccountDetail);
                }

                if (isExpenseTypeChanges == false) {

                    // Handle old project if it exists
                    Project oldProject = null;
                    if (oldExpense.getProjectId() != null && oldExpense.getProjectId() > 0) {
                        Optional<Project> projectOptional = projectRepo.findById(oldExpense.getProjectId());
                        if (projectOptional.isPresent()) {
                            oldProject = projectOptional.get();
                        }
                    }

                    Optional<VendorAccount> vendorAccountOptional = vendorAccountRepo.findById(oldExpense.getVendorAccountId());
                    if (vendorAccountOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Vendor Account!");

                    Optional<VendorPayment> vendorPaymentOptional = vendorAccountDetailRepo.findByExpenseId(oldExpense.getId());
                    if (vendorPaymentOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Vendor Account Detail!");


                    VendorAccount vendorAccount = vendorAccountOptional.get();
                    VendorPayment vendorPayment = vendorPaymentOptional.get();

                    if (!Objects.equals(newExpense.getVendorAccountId(), oldExpense.getVendorAccountId())) {

                        vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() - vendorPayment.getAmountPaid());
                        vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() - vendorPayment.getCreditAmount());
                        vendorAccount.setTotalAmount(vendorAccount.getTotalAmountPaid() + vendorAccount.getTotalCreditAmount());

                        vendorAccountRepo.save(vendorAccount);
                        vendorAccountDetailRepo.delete(vendorPayment);

//                    TODO :: ADD NEW ROWS FOR VENDOR ACCOUNT AND DETAIL


                        Optional<VendorAccount> accountOptional = vendorAccountRepo.findById(newExpense.getVendorAccountId());
                        if (!accountOptional.isPresent())
                            throw new IllegalArgumentException("Invalid Vendor");

                        vendorAccount = accountOptional.get();
                        vendorAccount.setTotalAmount(vendorAccount.getTotalAmount() + newExpense.getTotalAmount());
                        vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + newExpense.getAmountPaid());
                        vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() + newExpense.getCreditAmount());
                        vendorAccountRepo.save(vendorAccount);


                        vendorPayment = new VendorPayment();
                        vendorPayment.setAmountPaid(newExpense.getAmountPaid());
                        vendorPayment.setOrganizationAccountId(newExpense.getOrganizationAccountId());
                        vendorPayment.setCreditAmount(newExpense.getCreditAmount());
                        vendorPayment.setProjectId(newExpense.getProjectId() != null ? newExpense.getProjectId() : 0L);
                        if (newExpense.getCreditAmount() == 0) {
                            vendorPayment.setTransactionType(TransactionType.DEBIT);
                        } else if (newExpense.getAmountPaid() == 0) {
                            vendorPayment.setTransactionType(TransactionType.CREDIT);
                        } else {
                            vendorPayment.setTransactionType(TransactionType.DEBIT_CREDIT);
                        }
                        vendorPayment.setVendorAccountId(newExpense.getVendorAccountId());
                        vendorPayment.setVendorAccountId(newExpense.getVendorAccountId());
                        vendorPayment.setExpenseId(newExpense.getId());
                        vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);


                        newExpense.setVendorName(vendorAccount.getName());

                    } else {

                        double totalPaidAmount = vendorAccount.getTotalAmountPaid() + (newExpense.getAmountPaid() - oldExpense.getAmountPaid());
                        double totalCreditAmount = vendorAccount.getTotalCreditAmount() + (newExpense.getCreditAmount() - oldExpense.getCreditAmount());
                        double totalAmount = vendorAccount.getTotalAmount() + (newExpense.getAmountPaid() - oldExpense.getAmountPaid()) + (newExpense.getCreditAmount() - oldExpense.getCreditAmount());

                        vendorAccount.setTotalAmount(totalAmount);
                        vendorAccount.setTotalAmountPaid(totalPaidAmount);
                        vendorAccount.setTotalCreditAmount(totalCreditAmount);

                        vendorAccountRepo.save(vendorAccount);

                        vendorPayment.setCreditAmount(newExpense.getCreditAmount());
                        vendorPayment.setAmountPaid(newExpense.getAmountPaid());
                        vendorPayment.setUpdatedBy(loggedInUser);

                        vendorAccountDetailRepo.save(vendorPayment);
                    }

                    // Handle project changes only if projects are involved
                    Long oldProjectId = oldExpense.getProjectId() != null ? oldExpense.getProjectId() : 0L;
                    Long newProjectId = newExpense.getProjectId() != null ? newExpense.getProjectId() : 0L;

                    if (!oldProjectId.equals(newProjectId)) {
                        // Project changed or added/removed

                        // Remove from old project if it existed
                        if (oldProject != null) {
                            oldProject.setTotalAmount(oldProject.getTotalAmount() - oldExpense.getTotalAmount());
                            oldProject.setConstructionAmount(oldProject.getConstructionAmount() - oldExpense.getTotalAmount());
                            projectRepo.save(oldProject);
                        }

                        // Add to new project if provided
                        if (newProjectId > 0) {
                            Optional<Project> newProjectOptional = projectRepo.findById(newProjectId);
                            if (newProjectOptional.isEmpty())
                                throw new IllegalArgumentException("Invalid Updated Project!");

                            Project newProject = newProjectOptional.get();

                            double updatedConstructionAmount = newProject.getConstructionAmount() + newExpense.getTotalAmount();
                            double updatedTotalAmount = newProject.getTotalAmount() + newExpense.getTotalAmount();

                            newProject.setTotalAmount(updatedTotalAmount);
                            newProject.setConstructionAmount(updatedConstructionAmount);
                            newProject.setUpdatedBy(loggedInUser);

                            projectRepo.save(newProject);

                            newExpense.setProjectName(newProject.getName());
                        }

                    } else if (oldProject != null) {
                        // Same project, update the amounts

                        double updatedConstructionAmount = oldProject.getConstructionAmount() + (newExpense.getTotalAmount() - oldExpense.getTotalAmount());
                        double updatedTotalAmount = oldProject.getTotalAmount() + (newExpense.getTotalAmount() - oldExpense.getTotalAmount());

                        oldProject.setUpdatedBy(loggedInUser);
                        oldProject.setConstructionAmount(updatedConstructionAmount);
                        oldProject.setTotalAmount(updatedTotalAmount);

                        projectRepo.save(oldProject);
                    }
                }


            } // handling type change
            else if (newExpense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.MISCELLANEOUS)) {

                if (!isExpenseTypeChanges) {
                    removeMiscellaneousVendorPayment(oldExpense, loggedInUser);
                }
                addMiscellaneousVendorPayment(newExpense, loggedInUser);

                Optional<OrganizationAccount> organizationAccountOptional = organizationAccoutRepo.findById(newExpense.getOrganizationAccountId());
                if (organizationAccountOptional.isEmpty())
                    throw new IllegalArgumentException("Invalid Organization Account!");


                Optional<OrganizationAccountDetail> organizationAccountDetailOptional = organizationAccountDetailRepo.findByExpenseId(newExpense.getId());
                if (organizationAccountDetailOptional.isEmpty())
                    throw new IllegalArgumentException("Invalid Organization Account Detail!");

                OrganizationAccount organizationAccount = organizationAccountOptional.get();
                OrganizationAccountDetail organizationAccountDetail = organizationAccountDetailOptional.get();

                if (!Objects.equals(newExpense.getOrganizationAccountId(), oldExpense.getOrganizationAccountId())) {

                    OrganizationAccount oldOrganizationAccount = organizationAccoutRepo
                            .findById(organizationAccountDetail.getOrganizationAcctId())
                            .orElseThrow(() -> new IllegalArgumentException("Invalid old Organization Account!"));
                    oldOrganizationAccount.setTotalAmount(
                            oldOrganizationAccount.getTotalAmount() + organizationAccountDetail.getAmount());
                    oldOrganizationAccount.setUpdatedBy(loggedInUser);
                    organizationAccoutRepo.save(oldOrganizationAccount);

                    organizationAccountDetailRepo.delete(organizationAccountDetail);

                    organizationAccountDetail = new OrganizationAccountDetail();

                    populateExpenseAccountDetail(organizationAccountDetail, newExpense);

                    organizationAccountService.deductFromOrgAcct(organizationAccountDetail, loggedInUser);


                } else {

                    double latestAmountOrg = organizationAccount.getTotalAmount() + (oldExpense.getAmountPaid() - newExpense.getAmountPaid());
                    if (latestAmountOrg < 0) {
                        throw new IllegalArgumentException("Not Enough Funds for this account");
                    }
                    organizationAccount.setTotalAmount(latestAmountOrg);
                    organizationAccount.setUpdatedBy(loggedInUser);
                    organizationAccoutRepo.save(organizationAccount);

                    populateExpenseAccountDetail(organizationAccountDetail, newExpense);
                    organizationAccountDetail.setAmount(newExpense.getAmountPaid());
                    organizationAccountDetail.setUpdatedBy(loggedInUser);
                    organizationAccountDetailRepo.save(organizationAccountDetail);
                }
            } // handling type change

            newExpense.setUpdatedBy(loggedInUser);
            newExpense.setPaymentStatus(getPaymentStatus(newExpense));

            newExpense = expenseRepo.save(newExpense);

            ExpenseDetail expenseDetail = new ExpenseDetail();
            expenseDetail.setExpenseId(newExpense.getId());
            expenseDetail.setAmountPaid(newExpense.getAmountPaid());
            expenseDetail.setOrganizationAccountId(newExpense.getOrganizationAccountId());
            expenseDetail.setOrganizationAccountTitle(newExpense.getOrgAccountTitle());
            expenseDetail.setExpenseTitle(newExpense.getExpenseTitle());
            if (newExpense.getPaymentType() != null) {
                expenseDetail.setPaymentType(newExpense.getPaymentType());
                expenseDetail.setPaymentDocNo(newExpense.getPaymentDocNo());
                expenseDetail.setPaymentDocDate(newExpense.getPaymentDocDate());
            } else if (!oldDetails.isEmpty()) {
                expenseDetail.setPaymentType(oldDetails.get(0).getPaymentType());
                expenseDetail.setPaymentDocNo(oldDetails.get(0).getPaymentDocNo());
                expenseDetail.setPaymentDocDate(oldDetails.get(0).getPaymentDocDate());
            }
            expenseDetail.setUpdatedBy(loggedInUser);
            expenseDetail.setCreatedBy(loggedInUser);
            expenseDetailRepo.save(expenseDetail);

            journalEntryService.reverseJournalEntriesForExpenseUpdate(oldExpense, loggedInUser);
            journalEntryService.createJournalEntryForExpenseUpdate(
                    newExpense, updatedOrganizationAccount, loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, newExpense);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    private void populateExpenseAccountDetail(
            OrganizationAccountDetail detail,
            Expense expense
    ) {
        detail.setExpenseId(expense.getId());
        detail.setOrganizationAcctId(expense.getOrganizationAccountId());
        detail.setAmount(expense.getAmountPaid());
        detail.setComments(expense.getComments() == null || expense.getComments().isBlank()
                ? "Expense: " + expense.getExpenseTitle()
                : expense.getComments());
        detail.setTransactionCategory(
                expense.getExpenseType() == com.rem.backend.enums.ExpenseType.CONSTRUCTION
                        ? TransactionCategory.CONSTRUCTION
                        : TransactionCategory.MISCALLENOUS);
        detail.setProjectId(0L);
        detail.setProjectName(null);
        detail.setUnitSerialNo(null);

        if (expense.getProjectId() != null && expense.getProjectId() > 0) {
            Project project = projectRepo
                    .findByProjectIdAndOrganizationIdAndIsActiveTrue(
                            expense.getProjectId(), expense.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Project!"));
            detail.setProjectId(project.getProjectId());
            detail.setProjectName(project.getName());
            expense.setProjectName(project.getName());
        }

        if (expense.getUnitId() != null && expense.getUnitId() > 0) {
            Unit unit = unitRepo.findById(expense.getUnitId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Unit!"));
            detail.setUnitSerialNo(unit.getSerialNo());
        }
    }

    private void setExpenseAccountGroup(Expense expense, String loggedInUser) {
        ChartOfAccount account = journalUtilities.findChartOfAccount(expense, loggedInUser);
        expense.setAccountGroupId(account.getAccountGroup().getId());
    }

    private void validateMiscellaneousVendor(Expense expense) {
        Long vendorId = expense.getVendorAccountId();
        if (vendorId == null || vendorId <= 0) {
            if (expense.getCreditAmount() > 0) {
                throw new IllegalArgumentException("A vendor is required for a miscellaneous expense with credit");
            }
            expense.setVendorAccountId(0L);
            expense.setVendorName(null);
            return;
        }
        VendorAccount vendor = vendorAccountRepo.findById(vendorId)
                .filter(account -> account.getOrganizationId() == expense.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Vendor Account"));
        expense.setVendorName(vendor.getName());
    }

    private void addMiscellaneousVendorPayment(Expense expense, String loggedInUser) {
        if (expense.getVendorAccountId() == null || expense.getVendorAccountId() <= 0) {
            return;
        }
        VendorAccount vendor = vendorAccountRepo.findById(expense.getVendorAccountId())
                .filter(account -> account.getOrganizationId() == expense.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Vendor Account"));
        vendor.setTotalAmount(vendor.getTotalAmount() + expense.getTotalAmount());
        vendor.setTotalAmountPaid(vendor.getTotalAmountPaid() + expense.getAmountPaid());
        vendor.setTotalCreditAmount(vendor.getTotalCreditAmount() + expense.getCreditAmount());
        vendor.setUpdatedBy(loggedInUser);
        vendorAccountRepo.save(vendor);

        VendorPayment payment = new VendorPayment();
        payment.setVendorAccountId(vendor.getId());
        payment.setExpenseId(expense.getId());
        payment.setOrganizationAccountId(expense.getOrganizationAccountId());
        payment.setProjectId(0);
        payment.setAmountPaid(expense.getAmountPaid());
        payment.setCreditAmount(expense.getCreditAmount());
        payment.setBalanceAmount(vendor.getTotalCreditAmount());
        payment.setVendorPaymentType(VendorPaymentType.DIRECT_PURCHASE);
        payment.setTransactionType(expense.getCreditAmount() == 0 ? TransactionType.DEBIT
                : expense.getAmountPaid() == 0 ? TransactionType.CREDIT : TransactionType.DEBIT_CREDIT);
        payment.setCreatedBy(loggedInUser);
        payment.setUpdatedBy(loggedInUser);
        vendorAccountDetailRepo.save(payment);
    }

    private void removeMiscellaneousVendorPayment(Expense expense, String loggedInUser) {
        Optional<VendorPayment> paymentOptional = vendorAccountDetailRepo.findByExpenseId(expense.getId());
        if (paymentOptional.isEmpty()) {
            return;
        }
        VendorPayment payment = paymentOptional.get();
        VendorAccount vendor = vendorAccountRepo.findById(payment.getVendorAccountId())
                .filter(account -> account.getOrganizationId() == expense.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Vendor Account"));
        vendor.setTotalAmount(vendor.getTotalAmount() - payment.getAmountPaid() - payment.getCreditAmount());
        vendor.setTotalAmountPaid(vendor.getTotalAmountPaid() - payment.getAmountPaid());
        vendor.setTotalCreditAmount(vendor.getTotalCreditAmount() - payment.getCreditAmount());
        vendor.setUpdatedBy(loggedInUser);
        vendorAccountRepo.save(vendor);
        vendorAccountDetailRepo.delete(payment);
    }

    private void validateExpenseCanBeModified(Expense expense) {
        if (expense.getPdcStatus() != null
                || (expense.getExpenseType() == com.rem.backend.enums.ExpenseType.CONSTRUCTION
                && vendorAccountDetailRepo.findByExpenseId(expense.getId())
                .map(payment -> payment.getVendorPaymentType() == VendorPaymentType.PDC_CLEARANCE)
                .orElse(false))) {
            throw new IllegalArgumentException("PDC-related expenses must be managed through the PDC workflow");
        }
    }

    private Page<Expense> populateLegacyAccountGroups(Page<Expense> expenses) {
        populateLegacyAccountGroups(expenses.getContent());
        return expenses;
    }

    private void populateLegacyAccountGroups(List<Expense> expenses) {
        Set<Long> ids = expenses.stream()
                .filter(expense -> expense.getExpenseType() == com.rem.backend.enums.ExpenseType.MISCELLANEOUS
                        && expense.getAccountGroupId() == null && expense.getExpenseCOAId() > 0)
                .map(Expense::getExpenseCOAId)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, ChartOfAccount> accounts = coaRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(ChartOfAccount::getId, Function.identity()));
        for (Expense expense : expenses) {
            ChartOfAccount account = accounts.get(expense.getExpenseCOAId());
            if (expense.getExpenseType() == com.rem.backend.enums.ExpenseType.MISCELLANEOUS
                    && expense.getAccountGroupId() == null && account != null
                    && account.getOrganization() != null
                    && Objects.equals(expense.getOrganizationId(), account.getOrganization().getOrganizationId())
                    && account.getAccountGroup() != null) {
                expense.setAccountGroupId(account.getAccountGroup().getId());
            }
        }
    }

    @Transactional
    public Map<String, Object> deleteExpense(long expenseId, String loggedInUser) {

        try {

            Optional<Expense> expenseOptional = expenseRepo.findById(expenseId);

            if (expenseOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Expense");

            Expense expense = expenseOptional.get();

            if (expense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.HISTORICAL))
                throw new IllegalArgumentException("Historical Expense cannot be deleted!");
            validateExpenseCanBeModified(expense);

            ValidationService.validate(expense.getId(), "Expense");
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(expense.getExpenseType(), "newExpense type");
            ValidationService.validate(expense.getAmountPaid(), "amount paid");
            ValidationService.validate(expense.getTotalAmount(), "total amount");
            ValidationService.validate(expense.getOrganizationId(), "organization id");
            ValidationService.validate(expense.getOrganizationAccountId(), "organization account id");
            if (expenseDetailRepo.findByExpenseIdOrderByCreatedDateDesc(expenseId).size() > 1) {
                throw new IllegalArgumentException("Expenses with multiple payments cannot be deleted");
            }


            if (expense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {


                if (expense.getProjectId() != null && expense.getProjectId() > 0) {
                    Project project = projectRepo.findById(expense.getProjectId())
                            .orElseThrow(() -> new IllegalArgumentException("Invalid Project"));
                    project.setConstructionAmount(project.getConstructionAmount() - expense.getTotalAmount());
                    project.setTotalAmount(project.getTotalAmount() - expense.getTotalAmount());
                    project.setUpdatedBy(loggedInUser);
                    projectRepo.save(project);
                }

                VendorAccount account = vendorAccountRepo.findById(expense.getVendorAccountId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid Vendor Account"));
                account.setTotalAmount(account.getTotalAmount() - expense.getTotalAmount());
                account.setTotalCreditAmount(account.getTotalCreditAmount() - expense.getCreditAmount());
                account.setTotalAmountPaid(account.getTotalAmountPaid() - expense.getAmountPaid());
                account.setUpdatedBy(loggedInUser);
                vendorAccountRepo.save(account);
                vendorAccountDetailRepo.deleteByExpenseId(expense.getId());
            } else if (expense.getExpenseType() == com.rem.backend.enums.ExpenseType.MISCELLANEOUS) {
                removeMiscellaneousVendorPayment(expense, loggedInUser);
            }

            OrganizationAccount account = organizationAccoutRepo
                    .findByIdAndOrganizationId(expense.getOrganizationAccountId(), expense.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Organization Account"));
            account.setTotalAmount(account.getTotalAmount() + expense.getAmountPaid());
            account.setUpdatedBy(loggedInUser);
            organizationAccoutRepo.save(account);
            organizationAccountDetailRepo.deleteByExpenseId(expense.getId());

            journalEntryService.reverseJournalEntriesForExpenseUpdate(expense, loggedInUser);
            expenseDetailRepo.deleteByExpenseId(expense.getId());
            expenseRepo.delete(expense);


            return ResponseMapper.buildResponse(Responses.SUCCESS, "Successfully Deleted!");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // PAYING BACK TO VENDOR
    @Transactional
    public Map<String, Object> addExpenseDetail(ExpenseDetail expenseDetail, String loggedInUser) {

        try {

            ValidationService.validate(expenseDetail.getExpenseId(), "expense id");
            ValidationService.validate(expenseDetail.getAmountPaid(), "amount");
            ValidationService.validate(expenseDetail.getOrganizationAccountId(), "org account");
            ValidationService.validate(loggedInUser, "loggedInUser");

            Optional<Expense> expenseOptional = expenseRepo.findById(expenseDetail.getExpenseId());
            if (expenseOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Expense!");


            Optional<OrganizationAccount> organizationAccountOptional = organizationAccoutRepo.findById(expenseDetail.getOrganizationAccountId());
            if (organizationAccountOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Account!");

            Expense expense = expenseOptional.get();
            OrganizationAccount organizationAccount = organizationAccountOptional.get();

            if (expenseDetail.getAmountPaid() + expense.getAmountPaid() > expense.getTotalAmount())
                throw new IllegalArgumentException("Exceed to total Amount!");

            double lastCreditAmount = expense.getCreditAmount();
            expense.setAmountPaid(expense.getAmountPaid() + expenseDetail.getAmountPaid());
            expense.setCreditAmount(expense.getCreditAmount() - expenseDetail.getAmountPaid());
            expense.setPaymentStatus(getPaymentStatus(expense));
            expenseRepo.save(expense);

//          SETTLING ORG ACCOUNT
            OrganizationAccountDetail organizationAccountDetail = new OrganizationAccountDetail();
            organizationAccountDetail.setProjectId(expense.getProjectId());
            organizationAccountDetail.setComments("Paying Debt of " + expense.getVendorName() + " for " + expense.getExpenseTitle());
            organizationAccountDetail.setAmount(expenseDetail.getAmountPaid());
            organizationAccountDetail.setTransactionCategory(TransactionCategory.CUSTOMER_PAYMENT);
            organizationAccountDetail.setOrganizationAcctId(expenseDetail.getOrganizationAccountId());
            organizationAccountService.deductFromOrgAcct(organizationAccountDetail, loggedInUser);


//          SETTLING VENDOR ACCOUNT
            Optional<VendorAccount> accountOptional = vendorAccountRepo.findById(expense.getVendorAccountId());
            if (!accountOptional.isPresent())
                throw new IllegalArgumentException("Invalid Vendor");

            VendorAccount vendorAccount = accountOptional.get();
            vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + expenseDetail.getAmountPaid());
            double remainingCreditBalance = vendorAccount.getTotalCreditAmount() - expenseDetail.getAmountPaid();
            vendorAccount.setTotalCreditAmount(remainingCreditBalance);
            vendorAccountRepo.save(vendorAccount);


            VendorPayment vendorPayment = new VendorPayment();
            vendorPayment.setAmountPaid(expenseDetail.getAmountPaid());
            vendorPayment.setBalanceAmount(remainingCreditBalance);
            vendorPayment.setOrganizationAccountId(expenseDetail.getOrganizationAccountId());
            vendorPayment.setVendorPaymentType(VendorPaymentType.DUE_CLEARANCE);
//            vendorPayment.setCreditAmount(lastCreditAmount - expenseDetail.getAmountPaid());
//            if (expense.getCreditAmount() - expenseDetail.getAmountPaid() > 0) {
//                vendorPayment.setTransactionType(TransactionType.DEBIT_CREDIT);
//            } else {
//            }

            vendorPayment.setProjectId(expense.getProjectId());
            vendorPayment.setTransactionType(TransactionType.DEBIT);
            vendorPayment.setVendorAccountId(expense.getVendorAccountId());
            vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);


            expenseDetail.setExpenseTitle(expense.getExpenseTitle());
            expenseDetail.setOrganizationAccountTitle(organizationAccount.getName());
            expenseDetail.setCreatedBy(loggedInUser);
            expenseDetail.setUpdatedBy(loggedInUser);

            if (!expenseDetail.getPaymentType().equals(PaymentType.CHEQUE) &&
                    !expenseDetail.getPaymentType().equals(PaymentType.PAY_ORDER)) {
                expenseDetail.setPaymentDocDate(null);
                expenseDetail.setPaymentDocNo(null);
            }

            expenseDetailRepo.save(expenseDetail);

            // Create journal entry for expense detail (double-entry bookkeeping)
            journalEntryService.createJournalEntryForExpenseDetail(expense, organizationAccount,
                    expenseDetail.getAmountPaid(), loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseDetail);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> addExpenseType(ExpenseType expense, String loggedInUser) {

        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(expense.getOrganizationId(), "organization");
            ValidationService.validate(expense.getName(), "expense type");

            boolean exists = expenseTypeRepo.existsByNameContainingIgnoreCaseAndOrganizationId(expense.getName(), expense.getOrganizationId());
            if (exists)
                throw new IllegalArgumentException("Expense type already exists");


            expense.setCreatedBy(loggedInUser);
            expense.setUpdatedBy(loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseTypeRepo.save(expense));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> updateExpenseType(ExpenseType expense, String loggedInUser) {

        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(expense.getOrganizationId(), "organization");
            ValidationService.validate(expense.getName(), "expense type");


            expense.setCreatedBy(loggedInUser);
            expense.setUpdatedBy(loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseTypeRepo.save(expense));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAllExpenseType(long orgId, Pageable pageable) {
        try {
            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseTypeRepo.findAllByOrganizationId(orgId, pageable));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getAllExpenseType(long orgId) {
        try {
            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseTypeRepo.findAllByOrganizationId(orgId));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getExpenseTypeById(long id) {

        try {

            ValidationService.validate(id, "expense");

            Optional<ExpenseType> expenseType = expenseTypeRepo.findById(id);

            if (expenseType.isEmpty())
                throw new IllegalArgumentException("Invalid Expense Type");

            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseType.get());
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getExpenseById(long id) {

        try {

            ValidationService.validate(id, "expense");

            Optional<Expense> expenseOptional = expenseRepo.findById(id);

            if (expenseOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Expense");

            Expense expense = expenseOptional.get();
            populateLegacyAccountGroups(List.of(expense));
            return ResponseMapper.buildResponse(Responses.SUCCESS, expense);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
