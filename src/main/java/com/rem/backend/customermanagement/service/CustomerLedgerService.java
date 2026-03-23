package com.rem.backend.customermanagement.service;

import com.rem.backend.customermanagement.dto.*;
import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.entity.customer.CustomerPaymentDetail;
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.repository.*;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.repository.UserRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerLedgerService {

    private final UserRepo userRepo;
    private final CustomerRepo customerRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerPaymentRepo customerPaymentRepo;
    private final CustomerPaymentDetailRepo customerPaymentDetailRepo;
    private final UnitRepo unitRepo;
    private final ProjectRepo projectRepo;
    private final com.rem.backend.service.CustomerPaymentService customerPaymentService;

    /**
     * Get complete customer ledger (all transactions across all accounts)
     * Sorted by createdDate globally (not grouped by account)
     */
    public Map<String, Object> getCustomerLedger(String username, Pageable pageable) {
        try {
            Customer customer = getCustomerFromUsername(username);

            List<CustomerAccount> accounts = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(customer.getCustomerId());
            List<Long> accountIds = accounts.stream().map(CustomerAccount::getId).toList();

            if (accountIds.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, Collections.emptyList());
            }

            List<CustomerLedgerDTO> ledgerEntries = new ArrayList<>();
            double totalAmountSum = 0.0;

            // Calculate sum of all account totals
            for (CustomerAccount account : accounts) {
                totalAmountSum += account.getTotalAmount();
            }

            // Fetch all payment details across all accounts
            for (Long accountId : accountIds) {
                // Use existing getAllPaymentDetailsByAccountId functionality for each account
                Map<String, Object> paymentDetailsResponse = customerPaymentService.getAllPaymentDetailsByAccountId(accountId);

                if ("0000".equals(paymentDetailsResponse.get("responseCode"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseData = (Map<String, Object>) paymentDetailsResponse.get("data");

                    if (responseData != null) {
                        @SuppressWarnings("unchecked")
                        List<CustomerPaymentDetail> paymentDetails = (List<CustomerPaymentDetail>) responseData.get("paymentDetails");

                        if (paymentDetails != null && !paymentDetails.isEmpty()) {
                            // Convert payment details to ledger entries for this account
                            for (CustomerPaymentDetail detail : paymentDetails) {
                                CustomerLedgerDTO ledgerEntry = createLedgerEntryFromPaymentDetail(detail, accountId);
                                if (ledgerEntry != null) {
                                    ledgerEntries.add(ledgerEntry);
                                }
                            }
                        }
                    }
                }
            }

            // Sort all ledger entries by createdDate asc (oldest first) globally across all accounts
            ledgerEntries.sort(Comparator.comparing(CustomerLedgerDTO::getTransactionDate));

            // Calculate remaining balance using sum of all account totals
            calculateRunningBalanceForAccount(ledgerEntries, totalAmountSum);

            // Create response with ledger entries and total summary
            Map<String, Object> response = new HashMap<>();
            response.put("ledgerEntries", ledgerEntries);
            response.put("totalAmountSum", totalAmountSum);
            response.put("totalAccounts", accounts.size());
            response.put("totalTransactions", ledgerEntries.size());

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get ledger for specific account
     */
    public Map<String, Object> getAccountLedger(String username, Long accountId, Pageable pageable) {
        try {
            Customer customer = getCustomerFromUsername(username);

            // Verify account belongs to customer
            Optional<CustomerAccount> accountOpt = customerAccountRepo.findById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Account not found");
            }

            CustomerAccount account = accountOpt.get();
            if (account.getCustomer().getCustomerId() != customer.getCustomerId()) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Account does not belong to this customer");
            }

            // Use existing getAllPaymentDetailsByAccountId functionality
            Map<String, Object> paymentDetailsResponse = customerPaymentService.getAllPaymentDetailsByAccountId(accountId);

            // Extract payment details from the response
            @SuppressWarnings("unchecked")
            Map<String, Object> responseData = (Map<String, Object>) paymentDetailsResponse.get("data");

            if (responseData == null) {
                return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, "Invalid response data from payment service");
            }

            @SuppressWarnings("unchecked")
            List<CustomerPaymentDetail> paymentDetails = (List<CustomerPaymentDetail>) responseData.get("paymentDetails");

            if (paymentDetails == null || paymentDetails.isEmpty()) {
                // Return empty ledger entries if no payment details found
                Map<String, Object> enhancedResponse = new HashMap<>();
                enhancedResponse.put("ledgerEntries", new ArrayList<>());
                enhancedResponse.put("totalAmount", responseData.get("totalAmount"));
                enhancedResponse.put("grandTotal", responseData.get("grandTotal"));
                enhancedResponse.put("balanceAmount", responseData.get("balanceAmount"));
                enhancedResponse.put("customer", responseData.get("customer"));
                return ResponseMapper.buildResponse(Responses.SUCCESS, enhancedResponse);
            }

            List<CustomerLedgerDTO> ledgerEntries = new ArrayList<>();

            // Convert payment details to ledger entries
            for (CustomerPaymentDetail detail : paymentDetails) {
                CustomerLedgerDTO ledgerEntry = createLedgerEntryFromPaymentDetail(detail, accountId);
                if (ledgerEntry != null) {
                    ledgerEntries.add(ledgerEntry);
                }
            }

            // Always sort by createdDate asc (oldest first) for proper chronological order
            ledgerEntries.sort(Comparator.comparing(CustomerLedgerDTO::getTransactionDate));

            // Calculate remaining balance (entries are now in chronological order)
            Object totalAmountObj = responseData.get("totalAmount");
            if (totalAmountObj == null) {
                return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, "Total amount not found in response");
            }

            double totalAmount = ((Number) totalAmountObj).doubleValue();
            calculateRunningBalanceForAccount(ledgerEntries, totalAmount);

            // Keep entries sorted by createdDate ASC (oldest first) for final response

            // Create enhanced response with ledger entries and account summary
            Map<String, Object> enhancedResponse = new HashMap<>();
            enhancedResponse.put("ledgerEntries", ledgerEntries);
            enhancedResponse.put("totalAmount", responseData.get("totalAmount"));
            enhancedResponse.put("grandTotal", responseData.get("grandTotal"));
            enhancedResponse.put("balanceAmount", responseData.get("balanceAmount"));
            enhancedResponse.put("customer", responseData.get("customer"));

            return ResponseMapper.buildResponse(Responses.SUCCESS, enhancedResponse);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get ledger by payment type
     */
    public Map<String, Object> getLedgerByPaymentType(String username, String paymentType, Pageable pageable) {
        try {
            Customer customer = getCustomerFromUsername(username);

            List<CustomerAccount> accounts = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(customer.getCustomerId());
            List<Long> accountIds = accounts.stream().map(CustomerAccount::getId).toList();

            if (accountIds.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, Collections.emptyList());
            }

            List<CustomerLedgerDTO> ledgerEntries = new ArrayList<>();
            double totalAmountSum = 0.0;

            // Calculate sum of all account totals
            for (CustomerAccount account : accounts) {
                totalAmountSum += account.getTotalAmount();
            }

            for (Long accountId : accountIds) {
                List<CustomerPayment> payments = customerPaymentRepo.findByCustomerAccountId(accountId);

                for (CustomerPayment payment : payments) {
                    List<CustomerPaymentDetail> details = customerPaymentDetailRepo.findByCustomerPaymentId(payment.getId());

                    // Filter by payment type
                    List<CustomerPaymentDetail> filteredDetails = details.stream()
                        .filter(detail -> detail.getPaymentType().name().equals(paymentType))
                        .toList();

                    for (CustomerPaymentDetail detail : filteredDetails) {
                        CustomerLedgerDTO ledgerEntry = createLedgerEntry(payment, detail, accountId);
                        if (ledgerEntry != null) {
                            ledgerEntries.add(ledgerEntry);
                        }
                    }
                }
            }

            // Sort by transaction date (oldest first)
            ledgerEntries.sort(Comparator.comparing(CustomerLedgerDTO::getTransactionDate));

            // Calculate remaining balance using sum of all account totals
            calculateRunningBalanceForAccount(ledgerEntries, totalAmountSum);

            return ResponseMapper.buildResponse(Responses.SUCCESS, ledgerEntries);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get ledger summary
     */
    public Map<String, Object> getLedgerSummary(String username) {
        try {
            Customer customer = getCustomerFromUsername(username);

            List<CustomerAccount> accounts = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(customer.getCustomerId());
            List<Long> accountIds = accounts.stream().map(CustomerAccount::getId).toList();

            Map<String, Object> summary = new HashMap<>();

            if (!accountIds.isEmpty()) {
                // Calculate total amounts from accounts
                double totalAmount = accounts.stream().mapToDouble(CustomerAccount::getTotalAmount).sum();

                List<CustomerPayment> allPayments = customerPaymentRepo.findByCustomerAccountIdIn(accountIds);
                List<Long> paymentIds = allPayments.stream().map(CustomerPayment::getId).toList();

                if (!paymentIds.isEmpty()) {
                    List<CustomerPaymentDetail> allDetails = customerPaymentDetailRepo.findByCustomerPaymentIdIn(paymentIds);

                    // Calculate totals
                    double totalPaidAmount = allDetails.stream().mapToDouble(CustomerPaymentDetail::getAmount).sum();
                    double totalRemainingBalance = totalAmount - totalPaidAmount;
                    int totalTransactions = allDetails.size();

                    // Group by payment type
                    Map<String, Double> paymentTypeBreakdown = allDetails.stream()
                        .collect(Collectors.groupingBy(
                            detail -> detail.getPaymentType().name(),
                            Collectors.summingDouble(CustomerPaymentDetail::getAmount)
                        ));

                    summary.put("totalPaidAmount", totalPaidAmount);
                    summary.put("totalRemainingBalance", totalRemainingBalance);
                    summary.put("totalDebits", 0.0); // No debits in current system
                    summary.put("totalTransactions", totalTransactions);
                    summary.put("paymentTypeBreakdown", paymentTypeBreakdown);
                } else {
                    summary.put("totalPaidAmount", 0.0);
                    summary.put("totalRemainingBalance", totalAmount);
                    summary.put("totalDebits", 0.0);
                    summary.put("totalTransactions", 0);
                    summary.put("paymentTypeBreakdown", new HashMap<>());
                }
            } else {
                summary.put("totalPaidAmount", 0.0);
                summary.put("totalRemainingBalance", 0.0);
                summary.put("totalDebits", 0.0);
                summary.put("totalTransactions", 0);
                summary.put("paymentTypeBreakdown", new HashMap<>());
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, summary);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Create ledger entry from payment detail
     */
    private CustomerLedgerDTO createLedgerEntry(CustomerPayment payment, CustomerPaymentDetail detail, Long accountId) {
        return createLedgerEntryFromPaymentDetail(detail, accountId);
    }

    /**
     * Create ledger entry from payment detail (enhanced version)
     */
    private CustomerLedgerDTO createLedgerEntryFromPaymentDetail(CustomerPaymentDetail detail, Long accountId) {
        try {
            CustomerLedgerDTO ledgerEntry = new CustomerLedgerDTO();

            ledgerEntry.setId(detail.getId());
            ledgerEntry.setTransactionType("PAYMENT_IN");
            ledgerEntry.setReferenceType("PAYMENT_DETAIL");
            ledgerEntry.setReferenceId(detail.getId());
            ledgerEntry.setDescription("Payment received - " + detail.getCustomerPaymentReason().name());
            ledgerEntry.setDebitAmount(0.0);
            ledgerEntry.setCreditAmount(detail.getAmount());
            ledgerEntry.setTransactionDate(detail.getCreatedDate());
            ledgerEntry.setPaymentMode(detail.getPaymentType().name());
            ledgerEntry.setChequeNo(detail.getChequeNo());
            ledgerEntry.setChequeDate(detail.getChequeDate());
            ledgerEntry.setCreatedBy(detail.getCreatedBy());

            // Get account details
            Optional<CustomerAccount> accountOpt = customerAccountRepo.findById(accountId);
            if (accountOpt.isPresent()) {
                CustomerAccount account = accountOpt.get();
                ledgerEntry.setCustomerName(account.getCustomer().getName());

                // Get unit details
                Optional<Unit> unitOpt = unitRepo.findById(account.getUnit().getId());
                unitOpt.ifPresent(unit -> ledgerEntry.setUnitSerial(unit.getSerialNo()));

                // Get project details
                Optional<Project> projectOpt = projectRepo.findById(account.getProject().getProjectId());
                projectOpt.ifPresent(project -> ledgerEntry.setProjectName(project.getName()));
            }

            return ledgerEntry;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Calculate running balance for account ledger entries without re-sorting
     */
    private void calculateRunningBalanceForAccount(List<CustomerLedgerDTO> ledgerEntries, double totalAmount) {
        // Since entries are already sorted by date ASC (oldest first), calculate remaining balance directly
        double remainingBalance = totalAmount; // Start with total amount

        // Calculate remaining balances in chronological order (decreasing with each payment)
        for (CustomerLedgerDTO entry : ledgerEntries) {
            remainingBalance -= entry.getCreditAmount(); // Subtract payment amount from remaining balance
            entry.setRunningBalance(remainingBalance); // Set the remaining balance after this payment
        }
    }

    /**
     * Helper method to get customer from username
     */
    private Customer getCustomerFromUsername(String username) {
        ValidationService.validate(username, "username");

        Optional<User> userOpt = userRepo.findByUsernameAndIsActiveTrue(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        Optional<Customer> customerOpt = customerRepo.findByUserId(user.getId());
        if (customerOpt.isEmpty()) {
            throw new IllegalArgumentException("Customer profile not found for this user");
        }

        return customerOpt.get();
    }
}
