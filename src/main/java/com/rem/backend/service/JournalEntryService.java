package com.rem.backend.service;

import com.rem.backend.accountmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.accountmanagement.enums.TransactionCategory;
import com.rem.backend.dto.orgAccount.TransferFundRequest;
import com.rem.backend.entity.account.AccountGroup;
import com.rem.backend.entity.account.ChartOfAccount;
import com.rem.backend.entity.account.JournalDetailEntry;
import com.rem.backend.entity.account.JournalEntry;
import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.entity.customerpayable.CustomerPayable;
import com.rem.backend.entity.expense.Expense;
import com.rem.backend.accountmanagement.entity.OrganizationAccount;
import com.rem.backend.entity.vendor.VendorAccount;
import com.rem.backend.enums.*;
import com.rem.backend.repository.*;
import com.rem.backend.utility.JournalUtilities;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.rem.backend.utility.JournalUtilities.*;

@Service
@AllArgsConstructor
@Slf4j
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalDetailEntryRepository journalDetailEntryRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final AccountGroupRepository accountGroupRepository;
    private final OrganizationAccoutRepo organizationAccoutRepo;

    private final JournalUtilities journalUtilities;


    /**
     * Create journal entry for expense creation with payment
     * Double-entry bookkeeping: Total Debit MUST equal Total Credit
     * <p>
     * For cash payment (amountPaid > 0):
     * Debit: Expense Account (amountPaid)
     * Credit: Bank/Cash Account (amountPaid)
     * <p>
     * For credit purchase (creditAmount > 0):
     * Debit: Expense Account (creditAmount)
     * Credit: Accounts Payable - Vendor (creditAmount)
     */
    @Transactional
    public void createJournalEntryForExpense(Expense expense, OrganizationAccount organizationAccount, String loggedInUser) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // Create Journal Entry header
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(expense.getOrganizationId());
            journalEntry.setCreatedDate(expense.getCreatedDate() != null ? expense.getCreatedDate() : java.time.LocalDateTime.now());
            journalEntry.setReferenceType("EXPENSE");
            journalEntry.setExpenseId(expense.getId());
            journalEntry.setOrganizationAccountId(organizationAccount.getId());
            journalEntry.setVendorId(expense.getVendorAccountId());
            journalEntry.setProjectId(expense.getProjectId());
            journalEntry.setUnitId(expense.getUnitId());
            journalEntry.setDescription("Expense: " + expense.getExpenseTitle() +
                    (expense.getProjectName() != null ? " - Project: " + expense.getProjectName() : ""));
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for Expense ID: {}", journalEntry.getId(), expense.getId());

            // Find or create Chart of Account for Bank/Cash Account
            ChartOfAccount bankAccount = findBankAccount(organizationAccount.getId(), expense.getOrganizationId());
            log.info("Bank Account COA ID: {} - {}", bankAccount.getId(), bankAccount.getName());


            if (expense.getPaymentType().equals(PaymentType.CHEQUE)){

                // when cheque is cleared, we can record the payment directly to bank account. But if it's not cleared yet, we need to record the payment in a separate "Cheque Account" to reflect the pending nature of the transaction.
                if (expense.getPaymentStatus() != null && expense.getPaymentStatus().equals(PaymentStatus.PAID)){


                    ChartOfAccount chequeAccount = journalUtilities.getChartOfAccount(expense.getOrganizationId(), VENDOR_PAYABLE);
                    JournalDetailEntry debitChequeEntry = new JournalDetailEntry();
                    debitChequeEntry.setJournalEntryId(journalEntry.getId());
                    debitChequeEntry.setChartOfAccountId(chequeAccount.getId());
                    debitChequeEntry.setDebitAmount(expense.getTotalAmount());
                    debitChequeEntry.setCreditAmount(0.0);
                    debitChequeEntry.setDescription(
                            "Vendor payment settlement via cheque for expense: " + expense.getExpenseTitle()
                    );
                    detailEntries.add(debitChequeEntry);
                    totalDebit += expense.getAmountPaid();


                    // Find or create Accounts Payable account for vendor
                    ChartOfAccount accountsPayableAccount = journalUtilities.getChartOfAccount(expense.getOrganizationId(), VENDOR_PAYABLE);
                    log.info("Accounts Payable COA ID: {} - {}", accountsPayableAccount.getId(), accountsPayableAccount.getName());

                    JournalDetailEntry creditBankEntry = new JournalDetailEntry();
                    creditBankEntry.setJournalEntryId(journalEntry.getId());
                    creditBankEntry.setChartOfAccountId(bankAccount.getId());
                    creditBankEntry.setDebitAmount(0.0);
                    creditBankEntry.setCreditAmount(expense.getAmountPaid());
                    creditBankEntry.setDescription(
                            "Cheque payment issued from bank: " + organizationAccount.getName() +
                                    " for expense: " + expense.getExpenseTitle()
                    );
                    detailEntries.add(creditBankEntry);
                    totalCredit += expense.getAmountPaid();

                }else{ // when cheque is created but not yet cleared, we record the payment in a separate "Cheque Account" until it's cleared. This is to reflect the pending nature of the transaction.
                    ChartOfAccount chequeAccount = journalUtilities.getChartOfAccount(expense.getOrganizationId(), CONSTRUCTION_INVENTORY);
                    JournalDetailEntry debitChequeEntry = new JournalDetailEntry();
                    debitChequeEntry.setJournalEntryId(journalEntry.getId());
                    debitChequeEntry.setChartOfAccountId(chequeAccount.getId());
                    debitChequeEntry.setDebitAmount(expense.getTotalAmount());
                    debitChequeEntry.setCreditAmount(0.0);
                    debitChequeEntry.setDescription(
                            "Construction inventory purchased: " + expense.getExpenseTitle()
                    );
                    detailEntries.add(debitChequeEntry);
                    totalDebit += expense.getAmountPaid();


                    // Find or create Accounts Payable account for vendor
                    ChartOfAccount accountsPayableAccount = journalUtilities.getChartOfAccount(expense.getOrganizationId(), VENDOR_PAYABLE);
                    log.info("Accounts Payable COA ID: {} - {}", accountsPayableAccount.getId(), accountsPayableAccount.getName());

                    JournalDetailEntry creditBankEntry = new JournalDetailEntry();
                    creditBankEntry.setJournalEntryId(journalEntry.getId());
                    creditBankEntry.setChartOfAccountId(accountsPayableAccount.getId());
                    creditBankEntry.setDebitAmount(0.0);
                    creditBankEntry.setCreditAmount(expense.getAmountPaid());
                    creditBankEntry.setDescription(
                            "Vendor payable recorded for construction material purchase: " + expense.getExpenseTitle()
                    );
                    detailEntries.add(creditBankEntry);
                    totalCredit += expense.getAmountPaid();
                }

            }else{
                // If amountPaid > 0: Debit Expense, Credit Bank
                if (expense.getAmountPaid() > 0 && expense.getCreditAmount() == 0) {
                    // Debit: Expense Account

                    ChartOfAccount debitAccount =
                            expense.getExpenseType() != ExpenseType.CONSTRUCTION
                                    ? journalUtilities.findChartOfAccount(expense, loggedInUser)
                                    : journalUtilities.getChartOfAccount(expense.getOrganizationId(),CONSTRUCTION_INVENTORY);

                    if(debitAccount == null){
                        throw new RuntimeException("No Valid Debit Account was found");
                    }

                    log.info("Debit Account COA ID: {} - {}", debitAccount.getId(), debitAccount.getName());

                    JournalDetailEntry debitEntry = new JournalDetailEntry();
                    debitEntry.setJournalEntryId(journalEntry.getId());
                    debitEntry.setChartOfAccountId(debitAccount.getId());
                    debitEntry.setDebitAmount(expense.getTotalAmount());
                    debitEntry.setCreditAmount(0.0);
                    debitEntry.setDescription("Expense: " + expense.getExpenseTitle());
                    detailEntries.add(debitEntry);
                    totalDebit += expense.getTotalAmount();

                    // Credit: Bank Account
                    JournalDetailEntry creditEntry = new JournalDetailEntry();
                    creditEntry.setJournalEntryId(journalEntry.getId());
                    creditEntry.setChartOfAccountId(bankAccount.getId());
                    creditEntry.setDebitAmount(0.0);
                    creditEntry.setCreditAmount(expense.getAmountPaid());
                    creditEntry.setDescription("Payment from: " + organizationAccount.getName());
                    detailEntries.add(creditEntry);
                    totalCredit += expense.getAmountPaid();

                }

                // If creditAmount > 0: Debit Expense, Credit Accounts Payable (Vendor)
                else if (expense.getCreditAmount() > 0 && expense.getVendorAccountId() != null) {

                    ChartOfAccount constructionInventoryAccount = journalUtilities.getChartOfAccount
                            (organizationAccount.getOrganizationId(),CONSTRUCTION_INVENTORY);

                    // Find or create Accounts Payable account for vendor
                    ChartOfAccount accountsPayableAccount = journalUtilities.getChartOfAccount(expense.getOrganizationId(), VENDOR_PAYABLE);
                    log.info("Accounts Payable COA ID: {} - {}", accountsPayableAccount.getId(), accountsPayableAccount.getName());


                    JournalDetailEntry debitInventoryEntry = new JournalDetailEntry();
                    debitInventoryEntry.setJournalEntryId(journalEntry.getId());
                    debitInventoryEntry.setChartOfAccountId(constructionInventoryAccount.getId());
                    debitInventoryEntry.setDebitAmount(expense.getTotalAmount());
                    debitInventoryEntry.setCreditAmount(0.0);
                    debitInventoryEntry.setDescription("Expense: " + expense.getExpenseTitle());
                    detailEntries.add(debitInventoryEntry);
                    totalDebit += expense.getTotalAmount();


                    JournalDetailEntry creditVendorEntry = new JournalDetailEntry();
                    creditVendorEntry.setJournalEntryId(journalEntry.getId());
                    creditVendorEntry.setChartOfAccountId(accountsPayableAccount.getId());
                    creditVendorEntry.setDebitAmount(0.0);
                    creditVendorEntry.setCreditAmount(expense.getTotalAmount());
                    creditVendorEntry.setDescription("Expense: " + expense.getExpenseTitle());
                    detailEntries.add(creditVendorEntry);
                    totalCredit += expense.getTotalAmount();


                    JournalDetailEntry debitVendorEntry = new JournalDetailEntry();
                    debitVendorEntry.setJournalEntryId(journalEntry.getId());
                    debitVendorEntry.setChartOfAccountId(accountsPayableAccount.getId());
                    debitVendorEntry.setDebitAmount(expense.getAmountPaid());
                    debitVendorEntry.setCreditAmount(0.0);
                    debitVendorEntry.setDescription("Expense: " + expense.getExpenseTitle());
                    detailEntries.add(debitVendorEntry);
                    totalDebit += expense.getAmountPaid();


                    // Credit: Bank Account
                    JournalDetailEntry creditBankEntry = new JournalDetailEntry();
                    creditBankEntry.setJournalEntryId(journalEntry.getId());
                    creditBankEntry.setChartOfAccountId(bankAccount.getId());
                    creditBankEntry.setDebitAmount(0.0);
                    creditBankEntry.setCreditAmount(expense.getAmountPaid());
                    creditBankEntry.setDescription("Payment from: " + organizationAccount.getName());
                    detailEntries.add(creditBankEntry);
                    totalCredit += expense.getAmountPaid();
                }

            }


            // Validate double-entry: Debit MUST equal Credit
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException("Journal Entry imbalance! Debit: " + totalDebit + ", Credit: " + totalCredit);
            }

            // Save all detail entries
            for (JournalDetailEntry entry : detailEntries) {
                journalDetailEntryRepository.save(entry);
                log.info("Saved Journal Detail Entry - COA: {}, Debit: {}, Credit: {}",
                        entry.getChartOfAccountId(), entry.getDebitAmount(), entry.getCreditAmount());
            }

            log.info("Journal Entry {} completed. Total Debit: {}, Total Credit: {}",
                    journalEntry.getId(), totalDebit, totalCredit);

        } catch (Exception e) {
            log.error("Failed to create journal entry for expense {}: {}", expense.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create journal entry: " + e.getMessage(), e);
        }
    }

    /**
     * Create journal entry for additional expense payment (ExpenseDetail)
     * This is when paying off vendor credit/debt
     * <p>
     * Double-entry bookkeeping:
     * Debit: Accounts Payable - Vendor (reducing liability)
     * Credit: Bank/Cash Account (reducing asset)
     */
    @Transactional
    public void createJournalEntryForExpenseDetail(Expense expense, OrganizationAccount organizationAccount,
                                                   double paymentAmount, String loggedInUser) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // Create Journal Entry header
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(expense.getOrganizationId());
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("EXPENSE_PAYMENT");
            journalEntry.setExpenseId(expense.getId());
            journalEntry.setOrganizationAccountId(organizationAccount.getId());
            journalEntry.setDescription("Vendor Payment: " + expense.getExpenseTitle() +
                    " - Paying debt to " + expense.getVendorName());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for Expense Payment, Expense ID: {}",
                    journalEntry.getId(), expense.getId());

            // Find or create Chart of Account for Bank/Cash Account
            ChartOfAccount bankAccount = findBankAccount(organizationAccount.getId(), expense.getOrganizationId());

            // Find or create Accounts Payable account for vendor
            ChartOfAccount accountsPayableAccount = journalUtilities.getChartOfAccount(expense.getOrganizationId(), VENDOR_PAYABLE);

            // Debit: Accounts Payable (Vendor) - reducing the liability
            JournalDetailEntry debitEntry = new JournalDetailEntry();
            debitEntry.setJournalEntryId(journalEntry.getId());
            debitEntry.setChartOfAccountId(accountsPayableAccount.getId());
            debitEntry.setDebitAmount(paymentAmount);
            debitEntry.setCreditAmount(0.0);
            debitEntry.setDescription("Payment to: " + expense.getVendorName());
            detailEntries.add(debitEntry);
            totalDebit += paymentAmount;

            // Credit: Bank Account - reducing the asset
            JournalDetailEntry creditEntry = new JournalDetailEntry();
            creditEntry.setJournalEntryId(journalEntry.getId());
            creditEntry.setChartOfAccountId(bankAccount.getId());
            creditEntry.setDebitAmount(0.0);
            creditEntry.setCreditAmount(paymentAmount);
            creditEntry.setDescription("Payment from: " + organizationAccount.getName());
            detailEntries.add(creditEntry);
            totalCredit += paymentAmount;

            // Validate double-entry: Debit MUST equal Credit
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException("Journal Entry imbalance! Debit: " + totalDebit + ", Credit: " + totalCredit);
            }

            // Save all detail entries
            for (JournalDetailEntry entry : detailEntries) {
                journalDetailEntryRepository.save(entry);
                log.info("Saved Journal Detail Entry - COA: {}, Debit: {}, Credit: {}",
                        entry.getChartOfAccountId(), entry.getDebitAmount(), entry.getCreditAmount());
            }

            log.info("Journal Entry {} completed. Total Debit: {}, Total Credit: {}",
                    journalEntry.getId(), totalDebit, totalCredit);

        } catch (Exception e) {
            log.error("Failed to create journal entry for expense payment {}: {}", expense.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create journal entry: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void createVendorPaymentJournalEntry(VendorAccount vendorAccount,
                                                OrganizationAccount organizationAccount,
                                                double paymentAmount,
                                                String loggedInUser) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // Create Journal Entry header
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(vendorAccount.getOrganizationId());
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("VENDOR_PAYMENT");
            journalEntry.setExpenseId(0L);
            journalEntry.setOrganizationAccountId(organizationAccount.getId());
            journalEntry.setDescription("Vendor Payment: " +
                    " - Paying " + vendorAccount.getName());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for Vendor Payment, Vendor ID: {}",
                    journalEntry.getId(), vendorAccount.getId());

            // Get Bank account
            ChartOfAccount bankAccount = findBankAccount(organizationAccount.getId(), vendorAccount.getOrganizationId());
            if (bankAccount == null) throw new RuntimeException("Bank account not found");

            // Get Vendor Payable (AP) account
            ChartOfAccount accountsPayableAccount = journalUtilities
                    .getChartOfAccount(vendorAccount.getOrganizationId(), VENDOR_PAYABLE);

            // Debit AP (reduce liability)
            JournalDetailEntry debitAP = new JournalDetailEntry();
            debitAP.setJournalEntryId(journalEntry.getId());
            debitAP.setChartOfAccountId(accountsPayableAccount.getId());
            debitAP.setDebitAmount(paymentAmount);
            debitAP.setCreditAmount(0.0);
            debitAP.setDescription("Payment to vendor: " + vendorAccount.getName());
            detailEntries.add(debitAP);
            totalDebit += paymentAmount;

            // Credit Bank (reduce asset)
            JournalDetailEntry creditBank = new JournalDetailEntry();
            creditBank.setJournalEntryId(journalEntry.getId());
            creditBank.setChartOfAccountId(bankAccount.getId());
            creditBank.setDebitAmount(0.0);
            creditBank.setCreditAmount(paymentAmount);
            creditBank.setDescription("Payment from: " + organizationAccount.getName());
            detailEntries.add(creditBank);
            totalCredit += paymentAmount;

            // Validate double-entry
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException("Journal Entry imbalance! Debit: " + totalDebit + ", Credit: " + totalCredit);
            }

            // Save entries
            for (JournalDetailEntry entry : detailEntries) {
                journalDetailEntryRepository.save(entry);
                log.info("Saved Journal Detail Entry - COA: {}, Debit: {}, Credit: {}",
                        entry.getChartOfAccountId(), entry.getDebitAmount(), entry.getCreditAmount());
            }

            log.info("Vendor Payment Journal Entry {} completed. Total Debit: {}, Total Credit: {}",
                    journalEntry.getId(), totalDebit, totalCredit);

        } catch (Exception e) {
            log.error("Failed to create vendor payment journal entry for vendor {}: {}", vendorAccount.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create vendor payment journal entry: " + e.getMessage(), e);
        }
    }



    @Transactional
    public void createJournalEntryForBooking(Booking booking, String loggedInUser) {
        try {

            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // =========================
            // 1. JOURNAL HEADER
            // =========================
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(booking.getOrganizationId());
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("BOOKING");
            journalEntry.setBookingId(booking.getId());

            journalEntry.setOrganizationAccountId(0l);
            journalEntry.setCustomerId(booking.getCustomerId());
            journalEntry.setProjectId(booking.getProjectId());
            journalEntry.setUnitId(booking.getUnitId());

            journalEntry.setDescription(
                    "Booking created for Customer ID: " + booking.getCustomerId() +
                            " | Unit ID: " + booking.getUnitId() +
                            " | Project ID: " + booking.getProjectId()
            );

            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for Booking ID: {}",
                    journalEntry.getId(), booking.getId());

            // =========================
            // 2. COA FETCH
            // =========================

            ChartOfAccount customerReceivableAccount =
                    journalUtilities.getChartOfAccount(
                            booking.getOrganizationId(),
                            CUSTOMER_RECEIVABLE
                    );

            ChartOfAccount revenueAccount =
                    journalUtilities.getChartOfAccount(
                            booking.getOrganizationId(),
                            BOOKING_REVENUE
                    );

            log.info("COA - Receivable: {}, Revenue: {}",
                    customerReceivableAccount.getName(),
                    revenueAccount.getName());

            double amount = booking.getTotalAmount(); // IMPORTANT: must be set before call

            if (amount <= 0) {
                throw new RuntimeException("Booking total amount is invalid");
            }

            // =========================
            // 3. DEBIT - CUSTOMER RECEIVABLE
            // =========================
            JournalDetailEntry debitEntry = new JournalDetailEntry();
            debitEntry.setJournalEntryId(journalEntry.getId());
            debitEntry.setChartOfAccountId(customerReceivableAccount.getId());
            debitEntry.setDebitAmount(amount);
            debitEntry.setCreditAmount(0.0);

            debitEntry.setDescription(
                    "Booking receivable created for Customer ID: " +
                            booking.getCustomerId() +
                            " | Unit ID: " + booking.getUnitId()
            );

            detailEntries.add(debitEntry);
            totalDebit += amount;

            // =========================
            // 4. CREDIT - BOOKING REVENUE
            // =========================
            JournalDetailEntry creditEntry = new JournalDetailEntry();
            creditEntry.setJournalEntryId(journalEntry.getId());
            creditEntry.setChartOfAccountId(revenueAccount.getId());
            creditEntry.setDebitAmount(0.0);
            creditEntry.setCreditAmount(amount);

            creditEntry.setDescription(
                    "Booking revenue recognized for Customer ID: " +
                            booking.getCustomerId() +
                            " | Unit ID: " + booking.getUnitId() +
                            " | Project ID: " + booking.getProjectId()
            );

            detailEntries.add(creditEntry);
            totalCredit += amount;

            // =========================
            // 5. VALIDATION
            // =========================
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException(
                        "Booking Journal Entry imbalance! Debit: " +
                                totalDebit + ", Credit: " + totalCredit
                );
            }

            // =========================
            // 6. SAVE DETAILS
            // =========================
            for (JournalDetailEntry entry : detailEntries) {
                journalDetailEntryRepository.save(entry);

                log.info("Saved Journal Detail Entry - COA: {}, DR: {}, CR: {}",
                        entry.getChartOfAccountId(),
                        entry.getDebitAmount(),
                        entry.getCreditAmount());
            }

            log.info("Booking Journal Entry completed. ID: {}, Amount: {}",
                    journalEntry.getId(), amount);

        } catch (Exception e) {
            log.error("Failed booking journal entry for ID {}: {}",
                    booking.getId(), e.getMessage(), e);

            throw new RuntimeException(
                    "Failed to create booking journal entry: " + e.getMessage(), e
            );
        }
    }



    /**
     * Find or create Chart of Account for Bank/Cash Account
     * This links OrganizationAccount to ChartOfAccount for proper ledger entries
     */
    private ChartOfAccount findBankAccount(long organizationAccountId, long organizationId) {
        // Find existing account by name and organization

        Optional<AccountGroup> accountGroup = accountGroupRepository.
                findByNameAndOrganization_OrganizationId("bank/cash", organizationId);

        Optional<ChartOfAccount> existingAccount = chartOfAccountRepository
                .findAllByOrganization_OrganizationId(organizationId)
                .stream()
                .filter(coa -> coa.getOrganizationAccountId() != null && coa.getOrganizationAccountId() == organizationAccountId
                        && coa.getStatus() == AccountStatus.ACTIVE && coa.getAccountGroup().getId() ==
                        accountGroup.get().getId())
                .findFirst();

        return existingAccount.orElse(null);

    }

    /**
     * Journal Entry for Customer Payment Installment
     *
     * DR Bank / Cash
     * CR Booking Liability
     *
     */
    @Transactional
    public void createJournalEntryForCustomerPayment(
            CustomerAccount customerAccount,
            CustomerPayment customerPayment,
            List<OrganizationAccountDetail> organizationAccountDetails,
            Long unitId,
            Long bookingId,
            String loggedInUser
    ) {

        try {

            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            long organizationId = customerAccount.getCustomer().getOrganizationId();
            double amount = customerPayment.getAmount();

            // Customer Receivable account
            ChartOfAccount customerReceivableAccount =
                    journalUtilities.getChartOfAccount(organizationId, CUSTOMER_RECEIVABLE);

            // =========================
            // CASE 1: No bank account selected (null or empty)
            // =========================
            if (organizationAccountDetails == null || organizationAccountDetails.isEmpty()) {

                JournalEntry journalEntry = new JournalEntry();
                journalEntry.setOrganizationId(organizationId);
                journalEntry.setCreatedDate(customerPayment.getPaidDate());
                journalEntry.setReferenceType(TransactionCategory.CUSTOMER_PAYMENT.name());
                journalEntry.setOrganizationAccountId(0L);
                journalEntry.setUnitId(unitId);
                journalEntry.setBookingId(bookingId);
                journalEntry.setCustomerId(customerAccount.getCustomer().getCustomerId());
                journalEntry.setDescription("Customer payment received (no bank account). Payment ID: " + customerPayment.getId());
                journalEntry.setStatus(JournalEntryStatus.POSTED);
                journalEntry.setCreatedBy(loggedInUser);
                journalEntry = journalEntryRepository.save(journalEntry);

                // Debit: Undeposited Funds / Suspense
                ChartOfAccount undepositedAccount = journalUtilities.getChartOfAccount(organizationId, UNDEPOSITED_FUND_ACCOUNT);

                JournalDetailEntry debitEntry = new JournalDetailEntry();
                debitEntry.setJournalEntryId(journalEntry.getId());
                debitEntry.setChartOfAccountId(undepositedAccount.getId());
                debitEntry.setDebitAmount(amount);
                debitEntry.setCreditAmount(0.0);
                debitEntry.setDescription("Customer payment received (undeposited) for Booking ID: " + bookingId);
                detailEntries.add(debitEntry);
                totalDebit += amount;

                // Credit: Customer Receivable
                JournalDetailEntry creditEntry = new JournalDetailEntry();
                creditEntry.setJournalEntryId(journalEntry.getId());
                creditEntry.setChartOfAccountId(customerReceivableAccount.getId());
                creditEntry.setDebitAmount(0.0);
                creditEntry.setCreditAmount(amount);
                creditEntry.setDescription("Customer receivable reduced for Booking ID: " + bookingId);
                detailEntries.add(creditEntry);
                totalCredit += amount;

                validateAndSave(detailEntries, totalDebit, totalCredit);

                log.info("Customer Payment Journal Entry (no bank) completed: {}", journalEntry.getId());
                return;
            }

            // =========================
            // CASE 2: One or more bank accounts selected
            // =========================
            OrganizationAccountDetail firstDetail = organizationAccountDetails.get(0);

            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(organizationId);
            journalEntry.setCreatedDate(customerPayment.getPaidDate());
            journalEntry.setReferenceType(TransactionCategory.CUSTOMER_PAYMENT.name());
            journalEntry.setOrganizationAccountId(firstDetail.getOrganizationAcctId());
            journalEntry.setUnitId(unitId);
            journalEntry.setProjectId(firstDetail.getProjectId());
            journalEntry.setBookingId(bookingId);
            journalEntry.setCustomerId(customerAccount.getCustomer().getCustomerId());
            journalEntry.setDescription("Customer payment received. Payment ID: " + customerPayment.getId());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Journal Entry created: {}", journalEntry.getId());

            // Debit each bank account
            for (OrganizationAccountDetail detail : organizationAccountDetails) {
                ChartOfAccount bankAccount = findBankAccount(detail.getOrganizationAcctId(), organizationId);
                if (bankAccount == null) {
                    throw new RuntimeException("Bank account not found for org account ID: " + detail.getOrganizationAcctId());
                }

                double entryAmount = detail.getAmount();

                JournalDetailEntry debitEntry = new JournalDetailEntry();
                debitEntry.setJournalEntryId(journalEntry.getId());
                debitEntry.setChartOfAccountId(bankAccount.getId());
                debitEntry.setDebitAmount(entryAmount);
                debitEntry.setCreditAmount(0.0);
                debitEntry.setDescription("Customer payment received in bank for Booking ID: " + bookingId);
                detailEntries.add(debitEntry);
                totalDebit += entryAmount;
            }

            // Credit: Customer Receivable (full amount)
            JournalDetailEntry creditEntry = new JournalDetailEntry();
            creditEntry.setJournalEntryId(journalEntry.getId());
            creditEntry.setChartOfAccountId(customerReceivableAccount.getId());
            creditEntry.setDebitAmount(0.0);
            creditEntry.setCreditAmount(amount);
            creditEntry.setDescription("Customer receivable reduced for Booking ID: " + bookingId);
            detailEntries.add(creditEntry);
            totalCredit += amount;

            // Validate and save
            validateAndSave(detailEntries, totalDebit, totalCredit);

            log.info("Customer Payment Journal Entry completed: {}", journalEntry.getId());

        } catch (Exception e) {
            log.error("Failed Customer Payment Journal Entry: {}", e.getMessage(), e);
            throw new RuntimeException("Customer Payment Journal failed: " + e.getMessage(), e);
        }
    }


    @Transactional
    public void createJournalEntryForAccountDetail(
            OrganizationAccountDetail entry,
            Long organizationId,
            String loggedInUser) {

        String controlAccount;

    /*
     CREDIT = money going OUT of company account
     DEBIT = money coming IN company account
     */

        if(entry.getTransactionCategory().equals(TransactionCategory.CUSTOMER_PAYMENT)){
            return;
        }

        if (entry.getTransactionType() == TransactionType.CREDIT) {

            switch (entry.getTransactionCategory()) {

                case REFUND:
                    controlAccount = REFUND_ACCOUNT;
                    break;

                case WITHDRAWL:
                    controlAccount = EXPENSE_ACCOUNT;
                    break;

                case CONSTRUCTION:
                    controlAccount = CONSTRUCTION_ACCOUNT;
                    break;

                case ADJUSTMENT:
                    controlAccount = ADJUSTMENT_ACCOUNT;
                    break;

                case MISCALLENOUS:
                    controlAccount = MISC_ACCOUNT;
                    break;

                default:
                    return;
            }
            saveJournal(
                    controlAccount,
                    organizationId,
                    TransactionType.DEBIT,
                    entry,
                    loggedInUser
            );

        }


        else if (entry.getTransactionType() == TransactionType.DEBIT) {

            switch (entry.getTransactionCategory()) {

                case SCRAP_SALE:
                    controlAccount = SCRAP_INCOME_ACCOUNT;
                    break;

                case OTHER, MISCALLENOUS:
                    controlAccount = MISC_ACCOUNT;
                    break;

                case ADJUSTMENT:
                    controlAccount = ADJUSTMENT_ACCOUNT;
                    break;

                default:
                    return;
            }

            saveJournal(
                    controlAccount,
                    organizationId,
                    TransactionType.CREDIT,
                    entry,
                    loggedInUser
            );

        }
    }


    @Transactional
    private void saveJournal(
            String controlAccountName,
            long organizationId,
            TransactionType controlAccountType,
            OrganizationAccountDetail organizationAccountDetail,
            String loggedInUser) {


        ChartOfAccount companyAccount = findBankAccount(organizationAccountDetail.getOrganizationAcctId(),organizationId);


        ChartOfAccount controlAccount = journalUtilities.getChartOfAccount(organizationId,
                controlAccountName);

    /*
     Step 2: Create Journal Entry (HEADER)
     */

        JournalEntry journalEntry = new JournalEntry();

        journalEntry.setReferenceType(organizationAccountDetail.getTransactionCategory().name());

        journalEntry.setDescription(organizationAccountDetail.getComments());

        journalEntry.setCreatedBy(loggedInUser);

        journalEntry.setProjectId(organizationAccountDetail.getProjectId());

        journalEntry.setUnitId(Long.valueOf(organizationAccountDetail.getUnitSerialNo()));

        journalEntry.setCustomerId(organizationAccountDetail.getCustomerId());


        journalEntry.setStatus(JournalEntryStatus.POSTED);

        journalEntryRepository.save(journalEntry);



    /*
     Step 3: Control Account Detail
     */

        JournalDetailEntry controlDetail = new JournalDetailEntry();

        controlDetail.setJournalEntryId(journalEntry.getId());

        controlDetail.setChartOfAccountId(controlAccount.getId());

        controlDetail.setDebitAmount(
                controlAccountType == TransactionType.DEBIT
                        ? organizationAccountDetail.getAmount()
                        : 0
        );

        controlDetail.setCreditAmount(
                controlAccountType == TransactionType.CREDIT
                        ? organizationAccountDetail.getAmount()
                        : 0
        );


        journalDetailEntryRepository.save(controlDetail);



    /*
     Step 4: Company Account Detail (OPPOSITE ENTRY)
     */

        JournalDetailEntry companyDetail = new JournalDetailEntry();

        companyDetail.setJournalEntryId(journalEntry.getId());

        companyDetail.setChartOfAccountId(companyAccount.getId());

        companyDetail.setDebitAmount(
                controlAccountType == TransactionType.CREDIT
                        ? organizationAccountDetail.getAmount()
                        : 0
        );

        companyDetail.setCreditAmount(
                controlAccountType == TransactionType.DEBIT
                        ? organizationAccountDetail.getAmount()
                        : 0
        );

        journalDetailEntryRepository.save(companyDetail);

    }

    /**
     * Journal Entry for Booking Price Update.
     *
     * If new price > old price (price increase):
     *   DR Customer Receivable   (difference)
     *   CR Booking Revenue       (difference)
     *
     * If new price < old price (price decrease):
     *   DR Booking Revenue       (difference)
     *   CR Customer Receivable   (difference)
     */
    @Transactional
    public void createJournalEntryForBookingUpdate(
            Booking booking,
            double oldTotalAmount,
            double newTotalAmount,
            String loggedInUser
    ) {
        double difference = newTotalAmount - oldTotalAmount;
        if (Math.abs(difference) < 0.01) {
            log.info("Booking price unchanged for Booking ID: {}, skipping journal entry.", booking.getId());
            return;
        }

        try {
            double absDiff = Math.abs(difference);
            boolean isPriceIncrease = difference > 0;

            ChartOfAccount customerReceivableAccount =
                    journalUtilities.getChartOfAccount(booking.getOrganizationId(), CUSTOMER_RECEIVABLE);

            ChartOfAccount bookingRevenueAccount =
                    journalUtilities.getChartOfAccount(booking.getOrganizationId(), BOOKING_REVENUE);

            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(booking.getOrganizationId());
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("BOOKING_UPDATE");
            journalEntry.setBookingId(booking.getId());
            journalEntry.setOrganizationAccountId(0L);
            journalEntry.setCustomerId(booking.getCustomerId());
            journalEntry.setProjectId(booking.getProjectId());
            journalEntry.setUnitId(booking.getUnitId());
            journalEntry.setDescription(
                    "Booking price " + (isPriceIncrease ? "increased" : "decreased") +
                    " by " + absDiff +
                    " | Old: " + oldTotalAmount + " | New: " + newTotalAmount +
                    " | Booking ID: " + booking.getId()
            );
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            List<JournalDetailEntry> entries = new ArrayList<>();

            if (isPriceIncrease) {
                // DR Customer Receivable, CR Booking Revenue
                entries.add(buildEntry(journalEntry.getId(), customerReceivableAccount.getId(), absDiff, 0));
                entries.add(buildEntry(journalEntry.getId(), bookingRevenueAccount.getId(), 0, absDiff));
            } else {
                // DR Booking Revenue, CR Customer Receivable
                entries.add(buildEntry(journalEntry.getId(), bookingRevenueAccount.getId(), absDiff, 0));
                entries.add(buildEntry(journalEntry.getId(), customerReceivableAccount.getId(), 0, absDiff));
            }

            validateAndSave(entries, absDiff, absDiff);

            log.info("Booking Update Journal Entry completed. ID: {}, Diff: {}, Direction: {}",
                    journalEntry.getId(), absDiff, isPriceIncrease ? "INCREASE" : "DECREASE");

        } catch (Exception e) {
            log.error("Failed Booking Update Journal Entry for Booking {}: {}", booking.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create booking update journal entry: " + e.getMessage(), e);
        }
    }

//    @Transactional
//    public void createJournalEntryForBookingCancellation(
//            Long organizationId,
//            Booking booking,
//            double deposited,
//            double totalFees,
//            String loggedInUser
//    ) {
//
//        double totalDebit = 0;
//        double totalCredit = 0;
//
//        ChartOfAccount bookingLiability =
//                journalUtilities.getChartOfAccount(organizationId, BOOKING_LIABILITY);
//
//        ChartOfAccount cancellationRevenue =
//                journalUtilities.getChartOfAccount(organizationId, CANCELLATION_REVENUE_ACCOUNT);
//
//        ChartOfAccount refundPayable =
//                journalUtilities.getChartOfAccount(organizationId, REFUND_ACCOUNT);
//
//        double refund = deposited - totalFees;
//
//        JournalEntry journalEntry = new JournalEntry();
//        journalEntry.setOrganizationId(organizationId);
//        journalEntry.setReferenceType("BOOKING_CANCELLATION");
//        journalEntry.setBookingId(booking.getId());
//        journalEntry.setUnitId(booking.getUnitId());
//        journalEntry.setDescription("Booking cancelled. ID: " + booking.getId());
//        journalEntry.setStatus(JournalEntryStatus.POSTED);
//        journalEntry.setCreatedBy(loggedInUser);
//
//        journalEntry = journalEntryRepository.save(journalEntry);
//
//        List<JournalDetailEntry> entries = new ArrayList<>();
//
//        entries.add(buildEntry(journalEntry.getId(), bookingLiability.getId(), deposited, 0));
//        totalDebit += deposited;
//
//        if (totalFees > 0) {
//            entries.add(buildEntry(journalEntry.getId(), cancellationRevenue.getId(), 0, totalFees));
//            totalCredit += totalFees;
//        }
//
//        if (refund > 0) {
//            entries.add(buildEntry(journalEntry.getId(), refundPayable.getId(), 0, refund));
//            totalCredit += refund;
//        }
//
//        validateAndSave(entries, totalDebit, totalCredit);
//    }


    @Transactional
    public void createJournalEntryForBookingCancellation(
            Long organizationId,
            Booking booking,
            double deposited,   // total paid by customer
            double totalFees,   // cancellation charges
            String loggedInUser
    ) {

        try {

            double totalDebit = 0.0;
            double totalCredit = 0.0;

            double bookingAmount = booking.getTotalAmount(); // IMPORTANT
            double refund = deposited - totalFees;

            // =========================
            // 1. ACCOUNTS
            // =========================

            ChartOfAccount customerReceivable =
                    journalUtilities.getChartOfAccount(organizationId, CUSTOMER_RECEIVABLE);

            ChartOfAccount bookingRevenue =
                    journalUtilities.getChartOfAccount(organizationId, BOOKING_REVENUE);

            ChartOfAccount cancellationRevenue =
                    journalUtilities.getChartOfAccount(organizationId, CANCELLATION_REVENUE_ACCOUNT);

            ChartOfAccount refundPayable =
                    journalUtilities.getChartOfAccount(organizationId, REFUND_ACCOUNT);

            // =========================
            // 2. JOURNAL HEADER
            // =========================

            JournalEntry journalEntry = new JournalEntry();

            journalEntry.setOrganizationId(organizationId);
            journalEntry.setReferenceType("BOOKING_CANCELLATION");
            journalEntry.setBookingId(booking.getId());
            journalEntry.setUnitId(booking.getUnitId());
            journalEntry.setProjectId(booking.getProjectId());
            journalEntry.setCustomerId(booking.getCustomerId());

            journalEntry.setDescription("Booking cancelled. ID: " + booking.getId());

            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            List<JournalDetailEntry> entries = new ArrayList<>();

            // =========================
            // 3. REVERSE BOOKING
            // =========================
            // Dr Booking Revenue
            entries.add(buildEntry(
                    journalEntry.getId(),
                    bookingRevenue.getId(),
                    bookingAmount,
                    0
            ));
            totalDebit += bookingAmount;

            // Cr Customer Receivable
            entries.add(buildEntry(
                    journalEntry.getId(),
                    customerReceivable.getId(),
                    0,
                    bookingAmount
            ));
            totalCredit += bookingAmount;

            // =========================
            // 4. CANCELLATION FEES (income)
            // =========================
            if (totalFees > 0) {

                // Dr Customer Receivable
                entries.add(buildEntry(
                        journalEntry.getId(),
                        customerReceivable.getId(),
                        totalFees,
                        0
                ));
                totalDebit += totalFees;

                // Cr Cancellation Revenue
                entries.add(buildEntry(
                        journalEntry.getId(),
                        cancellationRevenue.getId(),
                        0,
                        totalFees
                ));
                totalCredit += totalFees;
            }

            // =========================
            // 5. REFUND PAYABLE (liability)
            // =========================
            if (refund > 0) {

                // Dr Customer Receivable
                entries.add(buildEntry(
                        journalEntry.getId(),
                        customerReceivable.getId(),
                        refund,
                        0
                ));
                totalDebit += refund;

                // Cr Refund Payable
                entries.add(buildEntry(
                        journalEntry.getId(),
                        refundPayable.getId(),
                        0,
                        refund
                ));
                totalCredit += refund;
            }

            // =========================
            // 6. VALIDATION
            // =========================
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException(
                        "Journal imbalance Debit: " + totalDebit +
                                " Credit: " + totalCredit
                );
            }

            // =========================
            // 7. SAVE
            // =========================
            for (JournalDetailEntry entry : entries) {
                journalDetailEntryRepository.save(entry);
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Booking cancellation journal failed: " + e.getMessage(),
                    e
            );
        }
    }



    @Transactional
    public void createJournalEntryForRefundPayment(
            Long organizationId,
            OrganizationAccountDetail orgAccountDetail,
            double amount,
            CustomerPayable customerPayable,
            String loggedInUser
    ) {

        double totalDebit = 0;
        double totalCredit = 0;

        ChartOfAccount bank =
                findBankAccount(orgAccountDetail.getOrganizationAcctId(), organizationId);

        ChartOfAccount refundPayable =
                journalUtilities.getChartOfAccount(organizationId, REFUND_ACCOUNT);

        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setOrganizationId(organizationId);
        journalEntry.setReferenceType("REFUND_PAYMENT");
        journalEntry.setBookingId(customerPayable.getBooking().getId());
        journalEntry.setDescription("Refund paid to customer");
        journalEntry.setStatus(JournalEntryStatus.POSTED);
        journalEntry.setCreatedBy(loggedInUser);

        journalEntry = journalEntryRepository.save(journalEntry);

        List<JournalDetailEntry> entries = new ArrayList<>();

        entries.add(buildEntry(journalEntry.getId(), refundPayable.getId(), amount, 0));
        totalDebit += amount;

        entries.add(buildEntry(journalEntry.getId(), bank.getId(), 0, amount));
        totalCredit += amount;

        validateAndSave(entries, totalDebit, totalCredit);
    }

    @Transactional
    public void internalFundTransfer(
            long organizationId,
            TransferFundRequest request,
            String loggedInUser) {


        ChartOfAccount fromAccount = findBankAccount(request.getFromAccountId(),organizationId);
        ChartOfAccount toAccount = findBankAccount(request.getToAccountId(),organizationId);


        JournalEntry journalEntry = new JournalEntry();

        journalEntry.setOrganizationId(fromAccount.getOrganization().getOrganizationId());

        journalEntry.setReferenceType("INTERNAL_FUND_TRANSFER");

        journalEntry.setDescription(
                "Fund transferred from " +
                        fromAccount.getName()
                        +
                        " to "
                        +
                        toAccount.getName()
        );

        journalEntry.setStatus(JournalEntryStatus.POSTED);

        journalEntry.setCreatedBy(loggedInUser);

        journalEntryRepository.save(journalEntry);


        JournalDetailEntry creditEntry = new JournalDetailEntry();

        creditEntry.setJournalEntryId(journalEntry.getId());

        creditEntry.setChartOfAccountId(fromAccount.getId());

        creditEntry.setCreditAmount(request.getAmount());

        creditEntry.setDebitAmount(0);

        creditEntry.setDescription("Fund Transfer Credit");

        journalDetailEntryRepository.save(creditEntry);


        JournalDetailEntry debitEntry = new JournalDetailEntry();

        debitEntry.setJournalEntryId(journalEntry.getId());

        debitEntry.setChartOfAccountId(toAccount.getId());

        debitEntry.setDebitAmount(request.getAmount());

        debitEntry.setCreditAmount(0);

        debitEntry.setDescription("Fund Transfer Debit");

        journalDetailEntryRepository.save(debitEntry);


    }


    /**
     * Journal Entry for posting already-received customer payment to organization bank account.
     * For each bank account selected:
     *   DR Bank/Cash
     *   CR Undeposited Funds (Customer Receivable)
     */
    @Transactional
    public void createJournalEntryForPaymentPosting(
            CustomerAccount customerAccount,
            CustomerPayment customerPayment,
            List<OrganizationAccountDetail> organizationAccountDetails,
            Long unitId,
            Long bookingId,
            String loggedInUser
    ) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            long organizationId = customerAccount.getCustomer().getOrganizationId();

            ChartOfAccount undepositedAccount =
                    journalUtilities.getChartOfAccount(organizationId, UNDEPOSITED_FUND_ACCOUNT);

            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(organizationId);
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("PAYMENT_POSTING");
            journalEntry.setOrganizationAccountId(organizationAccountDetails.get(0).getOrganizationAcctId());
            journalEntry.setUnitId(unitId);
            journalEntry.setProjectId(organizationAccountDetails.get(0).getProjectId());
            journalEntry.setBookingId(bookingId);
            journalEntry.setCustomerId(customerAccount.getCustomer().getCustomerId());
            journalEntry.setDescription("Payment posted to bank account. Payment ID: " + customerPayment.getId());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for Payment Posting, Payment ID: {}",
                    journalEntry.getId(), customerPayment.getId());

            for (OrganizationAccountDetail detail : organizationAccountDetails) {
                ChartOfAccount bankAccount = findBankAccount(detail.getOrganizationAcctId(), organizationId);
                if (bankAccount == null) {
                    throw new RuntimeException("Bank account not found for org account ID: " + detail.getOrganizationAcctId());
                }

                double entryAmount = detail.getAmount();

                // DR Bank
                JournalDetailEntry debitEntry = new JournalDetailEntry();
                debitEntry.setJournalEntryId(journalEntry.getId());
                debitEntry.setChartOfAccountId(bankAccount.getId());
                debitEntry.setDebitAmount(entryAmount);
                debitEntry.setCreditAmount(0.0);
                debitEntry.setDescription("Payment posted to bank for Booking ID: " + bookingId);
                detailEntries.add(debitEntry);
                totalDebit += entryAmount;

                // CR Undeposited Funds

            }


            JournalDetailEntry creditEntry = new JournalDetailEntry();
            creditEntry.setJournalEntryId(journalEntry.getId());
            creditEntry.setChartOfAccountId(undepositedAccount.getId());
            creditEntry.setDebitAmount(0.0);
            creditEntry.setCreditAmount(customerPayment.getAmount());
            creditEntry.setDescription("Undeposited funds cleared for Booking ID: " + bookingId);
            detailEntries.add(creditEntry);
            totalCredit += customerPayment.getAmount();

            validateAndSave(detailEntries, totalDebit, totalCredit);

            log.info("Payment Posting Journal Entry completed: {}", journalEntry.getId());

        } catch (Exception e) {
            log.error("Failed Payment Posting Journal Entry: {}", e.getMessage(), e);
            throw new RuntimeException("Payment Posting Journal failed: " + e.getMessage(), e);
        }
    }

    private JournalDetailEntry buildEntry(
            Long journalEntryId,
            Long chartOfAccountId,
            double debit,
            double credit
    ) {
        JournalDetailEntry entry = new JournalDetailEntry();

        entry.setJournalEntryId(journalEntryId);
        entry.setChartOfAccountId(chartOfAccountId);
        entry.setDebitAmount(debit);
        entry.setCreditAmount(credit);

        return entry;
    }


    private void validateAndSave(
            List<JournalDetailEntry> entries,
            double totalDebit,
            double totalCredit
    ) {
        if (Math.abs(totalDebit - totalCredit) > 0.01) {
            throw new RuntimeException(
                    "Journal imbalance Debit: " + totalDebit + " Credit: " + totalCredit
            );
        }

        for (JournalDetailEntry entry : entries) {
            journalDetailEntryRepository.save(entry);
        }
    }

}

