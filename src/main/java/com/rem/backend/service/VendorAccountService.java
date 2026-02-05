package com.rem.backend.service;

import com.rem.backend.entity.expense.Expense;
import com.rem.backend.entity.expense.ExpenseDetail;
import com.rem.backend.accountmanagement.entity.OrganizationAccount;
import com.rem.backend.accountmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.entity.organization.Organization;
import com.rem.backend.entity.vendor.VendorAccount;
import com.rem.backend.entity.vendor.VendorPayment;
import com.rem.backend.enums.ExpenseType;
import com.rem.backend.enums.TransactionType;
import com.rem.backend.enums.VendorPaymentType;
import com.rem.backend.repository.*;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.*;

import static com.rem.backend.utility.Utility.getPaymentStatus;

@Service
@AllArgsConstructor
public class VendorAccountService {

    private final VendorAccountRepo vendorAccountRepository;
    private final OrganizationRepo organizationRepo;
    private final VendorAccountDetailRepo vendorAccountDetailRepo;
    private final OrganizationAccoutRepo organizationAccoutRepo;
    private final OrganizationAccountDetailRepo organizationAccountDetailRepo;
    private final ExpenseRepo expenseRepo;
    private final ExpenseDetailRepo expenseDetailRepo;


    public Map<String, Object> getAllVendorAccounts(long orgId, Pageable pageable) {
        try {
            Page<VendorAccount> vendorAccounts = vendorAccountRepository.findAllByOrganizationId(orgId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccounts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    @Transactional
    public Map<String, Object> paybackCredit(VendorPayment request, String loggedInUser) {
        try {

            ValidationService.validate(request.getVendorAccountId(), "vendor");
            ValidationService.validate(request.getAmountPaid(), "amount");
            ValidationService.validate(request.getOrganizationAccountId(), "organization account");
            ValidationService.validate(request.getOrganizationId(), "organization");

            VendorAccount account = vendorAccountRepository.findById(request.getVendorAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Vendor"));


            Organization organization = organizationRepo.findById(request.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Organization"));

            if (!organization.isPaybackByVendor()){
                throw new IllegalArgumentException("Your organization does not support this");
            }

            if (account.getTotalCreditAmount() < request.getAmountPaid() || request.getAmountPaid() < 0) {
                throw new IllegalArgumentException("Amount cannot exceeded to credit amount");
            }

            Optional<VendorPayment> existingTxn =
                    vendorAccountDetailRepo.findByIdempotencyKey(request.getIdempotencyKey());

            if (existingTxn.isPresent()) {
                // IMPORTANT: return same response, no side effects
                return ResponseMapper.buildResponse(Responses.SUCCESS, existingTxn.get());
            }

            // If an organization account is supplied, deduct the amount from it and create an account detail
            Long orgAcctId = request.getOrganizationAccountId();
            if (orgAcctId != null && orgAcctId != 0) {
                OrganizationAccount orgAcct = organizationAccoutRepo.findById(orgAcctId)
                        .orElseThrow(() -> new IllegalArgumentException("Organization account not found"));

                double available = orgAcct.getTotalAmount();
                if (available < request.getAmountPaid()) {
                    throw new IllegalArgumentException("Insufficient funds in the organization account");
                }

                orgAcct.setTotalAmount(available - request.getAmountPaid());
                orgAcct.setUpdatedBy(loggedInUser);
                organizationAccoutRepo.save(orgAcct);

                OrganizationAccountDetail detail = new OrganizationAccountDetail();
                detail.setOrganizationAcctId(orgAcct.getId());
                detail.setAmount(request.getAmountPaid());
                detail.setComments("Vendor payback to " + account.getName());
                detail.setTransactionType(TransactionType.CREDIT); // deducted from org account
                detail.setCreatedBy(loggedInUser);
                detail.setUpdatedBy(loggedInUser);
                organizationAccountDetailRepo.save(detail);
            }

            double updatedCreditBalance = account.getTotalCreditAmount() - request.getAmountPaid();
            account.setTotalCreditAmount(updatedCreditBalance);
            account.setUpdatedBy(loggedInUser);
            vendorAccountRepository.save(account);

            VendorPayment vendorPayment = new VendorPayment();
            vendorPayment.setAmountPaid(request.getAmountPaid());
            vendorPayment.setOrganizationAccountId(request.getOrganizationAccountId());
            vendorPayment.setCreditAmount(0);
            vendorPayment.setBalanceAmount(updatedCreditBalance);
            vendorPayment.setVendorPaymentType(VendorPaymentType.DUE_CLEARANCE);
            vendorPayment.setTransactionType(TransactionType.DEBIT);
            vendorPayment.setVendorAccountId(account.getId());
            vendorPayment.setPaymentMethodType(request.getPaymentMethodType());
            vendorPayment.setUpdatedBy(loggedInUser);
            vendorPayment.setCreatedBy(loggedInUser);
            vendorPayment.setComments(request.getComments());
            vendorPayment.setIdempotencyKey(request.getIdempotencyKey());

            vendorAccountDetailRepo.save(vendorPayment);



            return ResponseMapper.buildResponse(Responses.SUCCESS, account);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // New: update an existing payback (DUE_CLEARANCE) payment. Handles increased/decreased amounts
    // and organization account changes (refund old, deduct new). Keeps transactional safety.
    @Transactional
    public Map<String, Object> updatePaybackCredit(Long paymentId, VendorPayment request, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(paymentId, "paymentId");
            ValidationService.validate(request.getAmountPaid(), "amount");
            ValidationService.validate(request.getOrganizationAccountId(), "organization account");
            ValidationService.validate(request.getOrganizationId(), "organization");

            VendorPayment existing = vendorAccountDetailRepo.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payback payment not found"));

            if (!VendorPaymentType.DUE_CLEARANCE.equals(existing.getVendorPaymentType())) {
                throw new IllegalArgumentException("Only DUE_CLEARANCE payments can be updated via this API");
            }

            // fetch vendor account
            VendorAccount vendorAccount = vendorAccountRepository.findById(existing.getVendorAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor account not found"));

            // validate organization
            Organization organization = organizationRepo.findById(request.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Organization"));
            if (!organization.isPaybackByVendor()) {
                throw new IllegalArgumentException("Your organization does not support this");
            }

            // primitives: amountPaid is a double (primitive) so cannot be null - use direct values
            double oldAmount = existing.getAmountPaid();
            double newAmount = request.getAmountPaid();
            double delta = newAmount - oldAmount; // positive => more deducted from org; negative => refund to org

            Long oldOrgAcctId = existing.getOrganizationAccountId();
            Long newOrgAcctId = request.getOrganizationAccountId();

            OrganizationAccount oldOrgAcct = null;
            OrganizationAccount newOrgAcct = null;

            if (oldOrgAcctId != null && oldOrgAcctId != 0) {
                oldOrgAcct = organizationAccoutRepo.findById(oldOrgAcctId)
                        .orElseThrow(() -> new IllegalArgumentException("Old organization account not found"));
            }
            if (newOrgAcctId != null && newOrgAcctId != 0) {
                newOrgAcct = organizationAccoutRepo.findById(newOrgAcctId)
                        .orElseThrow(() -> new IllegalArgumentException("New organization account not found"));
            }

            // if org account changed: refund old full amount and deduct new full amount
            if (oldOrgAcctId != null && !Objects.equals(oldOrgAcctId, newOrgAcctId)) {
                if (oldOrgAcct != null) {
                    double refunded = oldAmount;
                    double cur = oldOrgAcct.getTotalAmount();
                    oldOrgAcct.setTotalAmount((cur) + refunded);
                    oldOrgAcct.setUpdatedBy(loggedInUser);
                    organizationAccoutRepo.save(oldOrgAcct);

                    OrganizationAccountDetail refundDetail = new OrganizationAccountDetail();
                    refundDetail.setOrganizationAcctId(oldOrgAcct.getId());
                    refundDetail.setAmount(refunded);
                    refundDetail.setComments("Refund from organization account for payment update (vendor: " + vendorAccount.getName() + ")");
                    refundDetail.setTransactionType(TransactionType.DEBIT); // money added to old account
                    refundDetail.setCreatedBy(loggedInUser);
                    refundDetail.setUpdatedBy(loggedInUser);
                    organizationAccountDetailRepo.save(refundDetail);
                }

                if (newOrgAcct != null) {
                    double available = newOrgAcct.getTotalAmount();
                    if (available < newAmount) {
                        throw new IllegalArgumentException("Insufficient funds in the new organization account");
                    }
                    newOrgAcct.setTotalAmount(available - newAmount);
                    newOrgAcct.setUpdatedBy(loggedInUser);
                    organizationAccoutRepo.save(newOrgAcct);

                    OrganizationAccountDetail deductDetail = new OrganizationAccountDetail();
                    deductDetail.setOrganizationAcctId(newOrgAcct.getId());
                    deductDetail.setAmount(newAmount);
                    deductDetail.setComments("Deduct for vendor payback update (vendor: " + vendorAccount.getName() + ")");
                    deductDetail.setTransactionType(TransactionType.CREDIT);
                    deductDetail.setCreatedBy(loggedInUser);
                    deductDetail.setUpdatedBy(loggedInUser);
                    organizationAccountDetailRepo.save(deductDetail);
                }

            } else {
                // same org account (or both null/zero): apply delta on the single organization account
                OrganizationAccount target = newOrgAcct != null ? newOrgAcct : oldOrgAcct;
                if (target != null) {
                    double available = target.getTotalAmount();
                    if (delta > 0 && available < delta) {
                        throw new IllegalArgumentException("Insufficient funds in the organization account for increased amount");
                    }

                    // apply balance change
                    target.setTotalAmount(available - delta);
                    target.setUpdatedBy(loggedInUser);
                    organizationAccoutRepo.save(target);

                    // create account detail for delta
                    if (delta > 0) {
                        OrganizationAccountDetail deductDetail = new OrganizationAccountDetail();
                        deductDetail.setOrganizationAcctId(target.getId());
                        deductDetail.setAmount(delta);
                        deductDetail.setComments("Deduct for increased vendor payback (vendor: " + vendorAccount.getName() + ")");
                        deductDetail.setTransactionType(TransactionType.CREDIT);
                        deductDetail.setCreatedBy(loggedInUser);
                        deductDetail.setUpdatedBy(loggedInUser);
                        organizationAccountDetailRepo.save(deductDetail);
                    } else if (delta < 0) {
                        OrganizationAccountDetail refundDetail = new OrganizationAccountDetail();
                        refundDetail.setOrganizationAcctId(target.getId());
                        refundDetail.setAmount(-delta);
                        refundDetail.setComments("Refund for decreased vendor payback (vendor: " + vendorAccount.getName() + ")");
                        refundDetail.setTransactionType(TransactionType.DEBIT);
                        refundDetail.setCreatedBy(loggedInUser);
                        refundDetail.setUpdatedBy(loggedInUser);
                        organizationAccountDetailRepo.save(refundDetail);
                    }
                }
            }

            // adjust vendor account totals: vendorAccount currently reflects the old payment already,
            // so new total = current - delta
            double newVendorCredit = vendorAccount.getTotalCreditAmount() - delta;
            if (newVendorCredit < 0) {
                throw new IllegalArgumentException("Vendor credit cannot become negative");
            }
            vendorAccount.setTotalCreditAmount(newVendorCredit);
            vendorAccount.setUpdatedBy(loggedInUser);
            vendorAccountRepository.save(vendorAccount);

            // update payment record
            existing.setAmountPaid(newAmount);
            existing.setOrganizationAccountId(newOrgAcctId);
            existing.setBalanceAmount(vendorAccount.getTotalCreditAmount());
            existing.setComments(request.getComments());
            existing.setUpdatedBy(loggedInUser);
            vendorAccountDetailRepo.save(existing);

            return ResponseMapper.buildResponse(Responses.SUCCESS, existing);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAllVendorAccountsWithSearch(long orgId, String searchName, Pageable pageable) {
        try {
            Page<VendorAccount> vendorAccounts = vendorAccountRepository.findByOrganizationIdWithSearch(orgId, searchName, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccounts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAllVendorAccountsByOrg(long orgId) {
        try {
            List<Map<String, Object>> vendorAccounts = vendorAccountRepository.findAllByOrgId(orgId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccounts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getAccountById(long accountId) {
        try {
            Optional<VendorAccount> vendorAccounts = vendorAccountRepository.findById(accountId);
            if (vendorAccounts.isEmpty())
                throw new IllegalArgumentException("Invalid account!");

            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccounts.get());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getVendorDetailsByAccount(long acctId, Pageable pageable) {
        try {

            Optional<VendorAccount> vendorAccountOptional = vendorAccountRepository.findById(acctId);
            if (vendorAccountOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Vendor Account");


            Page<VendorPayment> vendorAccountDetail = vendorAccountDetailRepo.findByVendorAccountIdOrderByIdDesc(acctId, pageable);


            vendorAccountDetail.getContent().forEach(payment -> {

                        if (payment.getOrganizationAccountId() != null && payment.getOrganizationAccountId() != 0) {
                            Optional<OrganizationAccount> organizationAccountOptional = organizationAccoutRepo.findById(payment.getOrganizationAccountId());
                            payment.setOrganizationAccount(organizationAccountOptional.get().getName());
                        }

                        payment.setVendorAccount(vendorAccountOptional.get().getName());
                    }
            );

            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccountDetail);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getVendorDetailsByAccountWithoutPagination(long acctId) {

        Map<String, Object> response = new HashMap<>();
        try {

            Optional<VendorAccount> accountOptional = vendorAccountRepository.findById(acctId);
            if (accountOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Vendor Account");


            List<VendorPayment> vendorAccounts = vendorAccountDetailRepo.findByVendorAccountIdOrderByIdDesc(acctId);

            vendorAccounts.forEach(payment -> {
                        if (payment.getOrganizationAccountId() != null) {
                            Optional<OrganizationAccount> organizationAccountOptional = organizationAccoutRepo.findById(payment.getOrganizationAccountId());
                            payment.setOrganizationAccount(organizationAccountOptional.get().getName());
                        }
                        payment.setVendorAccount(accountOptional.get().getName());
                    }
            );

            VendorAccount account = accountOptional.get();

            response.put("list", vendorAccounts);
            response.put("totalPaid", account.getTotalAmountPaid());
            response.put("totalCredit", account.getTotalCreditAmount());
            response.put("totalAmount", account.getTotalAmount());


            return ResponseMapper.buildResponse(Responses.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ✅ Get all vendor accounts by name or get all
    public Map<String, Object> getAllVendorAccountsFilter(String nameFilter) {
        try {
            List<VendorAccount> vendorAccounts;
            if (nameFilter != null && !nameFilter.isBlank()) {
                vendorAccounts = vendorAccountRepository.findByNameContainingIgnoreCase(nameFilter);
            } else {
                vendorAccounts = vendorAccountRepository.findAll();
            }
            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccounts);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // ✅ Get all vendor accounts by name or get all
    public Map<String, Object> updatingBalanceAmount() {
        try {

            Optional<VendorAccount> optional = vendorAccountRepository.findById(57l);

            List<VendorPayment> vendorPayments =
                    vendorAccountDetailRepo.findByVendorAccountIdOrderByIdAsc(optional.get().getId());

            double paidAmount = 0, creditAmount = 0;
            vendorPayments.forEach(payment -> {


            });

            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorPayments);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // ✅ Get all vendor accounts by name or get all
    public List<VendorPayment> getVendorPaymentsAsc(long vendorAccountId) {

        Optional<VendorAccount> optional =
                vendorAccountRepository.findById(vendorAccountId);

        if (optional.isEmpty()) {
            return Collections.emptyList();
        }

        List<VendorPayment> payments =
                vendorAccountDetailRepo
                        .findByVendorAccountIdOrderByIdAsc(optional.get().getId());

        double runningBalance = 0.0;

        for (VendorPayment vp : payments) {

            if (VendorPaymentType.DIRECT_PURCHASE.equals(vp.getVendorPaymentType())) {
                runningBalance += vp.getCreditAmount();
            }

            if (VendorPaymentType.DUE_CLEARANCE.equals(vp.getVendorPaymentType())) {
                runningBalance -= vp.getAmountPaid();
            }

            // safety guard
            if (runningBalance < 0) {
                runningBalance = 0;
            }

            vp.setBalanceAmount(runningBalance);
        }

        // OPTIONAL: persist updated balances
        vendorAccountDetailRepo.saveAll(payments);

        return payments;
    }


    // ✅ Get a vendor account by ID
    public Map<String, Object> getVendorAccountById(long id) {
        try {
            ValidationService.validate(id, "vendorAccountId");
            Optional<VendorAccount> optional = vendorAccountRepository.findById(id);
            if (optional.isPresent()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, optional.get());
            } else {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Vendor account not found");
            }
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ✅ Create a vendor account
    @Transactional
    public Map<String, Object> createVendorAccount(VendorAccount vendorAccount, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(vendorAccount.getName(), "name");
            ValidationService.validate(vendorAccount.getOrganizationId(), "organization");

            vendorAccount.setCreatedBy(loggedInUser);
            vendorAccount.setUpdatedBy(loggedInUser);

            vendorAccount = vendorAccountRepository.save(vendorAccount);

            VendorPayment payment = new VendorPayment();

            payment.setVendorAccountId(vendorAccount.getId());
            payment.setUpdatedBy(loggedInUser);
            payment.setCreatedBy(loggedInUser);
            payment.setVendorAccount(vendorAccount.getName());
            payment.setCreditAmount(vendorAccount.getTotalCreditAmount());
            payment.setBalanceAmount(vendorAccount.getTotalCreditAmount());
            payment.setAmountPaid(vendorAccount.getTotalAmountPaid());
            payment.setVendorPaymentType(VendorPaymentType.DIRECT_PURCHASE);
            if (vendorAccount.getTotalAmountPaid() == vendorAccount.getTotalAmount())
                payment.setTransactionType(TransactionType.DEBIT);
            else if (vendorAccount.getTotalCreditAmount() == vendorAccount.getTotalAmount())
                payment.setTransactionType(TransactionType.CREDIT);
            else payment.setTransactionType(TransactionType.DEBIT_CREDIT);
            vendorAccountDetailRepo.save(payment);


            Expense expense = new Expense();
            expense.setVendorAccountId(vendorAccount.getId());
            expense.setOrganizationId(vendorAccount.getOrganizationId());
            expense.setVendorName(vendorAccount.getName());
            expense.setComments("Historical expense added during system onboarding");
            expense.setExpenseTitle("Historical expense added during system onboarding");
            expense.setExpenseType(ExpenseType.HISTORICAL);
            expense.setExpenseTypeId(0l);
            expense.setOrganizationAccountId(0l);
            expense.setProjectId(0l);
            expense.setCreditAmount(vendorAccount.getTotalCreditAmount());
            expense.setAmountPaid(vendorAccount.getTotalAmountPaid());
            expense.setTotalAmount(vendorAccount.getTotalCreditAmount() + vendorAccount.getTotalAmountPaid());
            expense.setCreatedBy(loggedInUser);
            expense.setUpdatedBy(loggedInUser);
            expense.setPaymentStatus(getPaymentStatus(expense));

            expense = expenseRepo.save(expense);

            vendorAccount.setHistoryExpenseId(expense.getId());
            vendorAccountRepository.save(vendorAccount);

            ExpenseDetail expenseDetail = new ExpenseDetail();
            expenseDetail.setExpenseTitle(expense.getExpenseTitle());
            expenseDetail.setExpenseId(expense.getId());
            expenseDetail.setAmountPaid(vendorAccount.getTotalAmountPaid());
            expenseDetail.setOrganizationAccountTitle("");
            expenseDetail.setOrganizationAccountId(0l);
            expenseDetail.setUpdatedBy(loggedInUser);
            expenseDetail.setCreatedBy(loggedInUser);

            expenseDetailRepo.save(expenseDetail);


            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccount);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    // ✅ Update a vendor account
    public Map<String, Object> updateVendorAccount(VendorAccount vendorAccount, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(vendorAccount.getId(), "vendor account");
            ValidationService.validate(vendorAccount.getName(), "name");
            ValidationService.validate(vendorAccount.getOrganizationId(), "organization");

            Optional<VendorAccount> vendorAccountOptional = vendorAccountRepository.findById(vendorAccount.getId());


            if (vendorAccountOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Vendor Account!");

            VendorAccount account = vendorAccountOptional.get();

            if (vendorAccount.getOrganizationId() != account.getOrganizationId())
                throw new IllegalArgumentException("Invalid Call!");


            if (isContainsTransactionHistory(vendorAccount.getId(), vendorAccount.getHistoryExpenseId()))
                throw new IllegalArgumentException("Cannot be edited because it has transaction history");

            if (vendorAccount.getHistoryExpenseId() != null) {
                Optional<Expense> expenseOptional = expenseRepo.findById(vendorAccount.getHistoryExpenseId());
                if (expenseOptional.isPresent()) {
                    Expense expense = expenseOptional.get();
                    expense.setCreditAmount(vendorAccount.getTotalCreditAmount());
                    expense.setAmountPaid(vendorAccount.getTotalAmountPaid());
                    expense.setTotalAmount(vendorAccount.getTotalAmountPaid() + vendorAccount.getTotalCreditAmount());
                    expense.setPaymentStatus(getPaymentStatus(expense));
                    expense.setUpdatedBy(loggedInUser);
                    expense = expenseRepo.save(expense);

                    List<ExpenseDetail> expenseDetailList = expenseDetailRepo.findByExpenseIdOrderByCreatedDateDesc(expense.getId());

                    for (ExpenseDetail expenseDetail : expenseDetailList) {
                        expenseDetail.setAmountPaid(vendorAccount.getTotalAmountPaid());
                        expenseDetail.setOrganizationAccountId(0l);
                        expenseDetail.setUpdatedBy(loggedInUser);
                        expenseDetailRepo.save(expenseDetail);

                    }


                }

            }

            vendorAccount.setCreatedBy(loggedInUser);
            vendorAccount.setUpdatedBy(loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccountRepository.save(vendorAccount));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public boolean isContainsTransactionHistory(long id, Long historyExpenseId) {
        try {
            List<Expense> expenseList = expenseRepo.findAllByVendorAccountId(id);
            List<Expense> expenseFilteredList = expenseList;

            if (historyExpenseId != null)
                expenseFilteredList = expenseList.stream().filter(expense -> !Objects.equals(expense.getId(), historyExpenseId)).toList();

            return expenseFilteredList.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }


    // ✅ Update a vendor account
    public Map<String, Object> deleteVendorAccount(Long vendorId) {
        try {
            ValidationService.validate(vendorId, "vendor account");

            Optional<VendorAccount> vendorAccountOptional = vendorAccountRepository.findById(vendorId);

            if (vendorAccountOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Vendor Account!");


            if (isContainsTransactionHistory(vendorId, vendorAccountOptional.get().getHistoryExpenseId()))
                throw new IllegalArgumentException("Cannot be deleted because it has transaction history");

            vendorAccountRepository.deleteById(vendorId);

            if (vendorAccountOptional.get().getHistoryExpenseId() != null) {
                expenseRepo.deleteById(vendorAccountOptional.get().getHistoryExpenseId());
                List<ExpenseDetail> expenseDetails = expenseDetailRepo.findByExpenseIdOrderByCreatedDateDesc(vendorAccountOptional.get().getHistoryExpenseId());
                expenseDetailRepo.deleteAllInBatch(expenseDetails);

            }


            return ResponseMapper.buildResponse(Responses.SUCCESS, "Successfully deleted!");
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> addPaymentHistory(VendorPayment vendorPayment, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(vendorPayment.getVendorAccountId(), "account");
            ValidationService.validate(vendorPayment.getTransactionType(), "transaction type");


//             TODO :: DO CALCUTALITION FOR DEBOT / CREDIT AND ALSO UPDATE VENDOR ACCOUNT
            vendorPayment.setCreatedBy(loggedInUser);
            vendorPayment.setUpdatedBy(loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccountDetailRepo.save(vendorPayment));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ✅ Update a vendor account
    public Map<String, Object> updateVendorAccount(long id, VendorAccount updatedAccount, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(id, "vendorAccountId");

            Optional<VendorAccount> optional = vendorAccountRepository.findById(id);
            if (!optional.isPresent()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Vendor account not found");
            }

            VendorAccount existing = optional.get();
            existing.setName(updatedAccount.getName());
            existing.setTotalAmountPaid(updatedAccount.getTotalAmountPaid());
            existing.setTotalCreditAmount(updatedAccount.getTotalCreditAmount());
            existing.setTotalBalanceAmount(updatedAccount.getTotalBalanceAmount());
            existing.setTotalAmount(updatedAccount.getTotalAmount());
            existing.setUpdatedBy(loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccountRepository.save(existing));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ✅ Delete a vendor account
    public Map<String, Object> deleteVendorAccount(long id) {
        try {
            ValidationService.validate(id, "vendorAccountId");

            if (!vendorAccountRepository.existsById(id)) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Vendor account not found");
            }

            vendorAccountRepository.deleteById(id);
            return ResponseMapper.buildResponse(Responses.SUCCESS, "Vendor account deleted");
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
