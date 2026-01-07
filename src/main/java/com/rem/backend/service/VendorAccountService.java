package com.rem.backend.service;

import com.rem.backend.entity.expense.Expense;
import com.rem.backend.entity.expense.ExpenseDetail;
import com.rem.backend.entity.organization.OrganizationAccount;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.rem.backend.utility.Utility.getPaymentStatus;

@Service
@AllArgsConstructor
public class VendorAccountService {

    private final VendorAccountRepo vendorAccountRepository;
    private final VendorAccountDetailRepo vendorAccountDetailRepo;
    private final OrganizationAccoutRepo organizationAccoutRepo;
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


            Page<VendorPayment> vendorAccountDetail = vendorAccountDetailRepo.findByVendorAccountId(acctId, pageable);


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

            double paidAmount = 0 , creditAmount  = 0;
            vendorPayments.forEach(payment -> {



            });

            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorPayments);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
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
                expenseFilteredList = expenseList.stream().filter(expense -> expense.getId() != historyExpenseId).toList();

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
