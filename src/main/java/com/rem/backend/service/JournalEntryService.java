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
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.vendor.VendorAccount;
import com.rem.backend.enums.*;
import com.rem.backend.payrollmanagement.entity.SalarySlip;
import com.rem.backend.propertymanagement.entity.PropertyPayment;
import com.rem.backend.propertymanagement.entity.PropertyPurchase;
import com.rem.backend.propertymanagement.repository.PropertyPurchaseRepo;
import com.rem.backend.purchasemanagement.entity.grn.Grn;
import com.rem.backend.purchasemanagement.entity.grn.GrnItems;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrderItem;
import com.rem.backend.purchasemanagement.entity.vendorinvoice.VendorInvoice;
import com.rem.backend.purchasemanagement.entity.vendorinvoice.VendorInvoiceItem;
import com.rem.backend.purchasemanagement.entity.vendorpayment.VendorPaymentPO;
import com.rem.backend.repository.*;
import com.rem.backend.utility.JournalUtilities;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final PropertyPurchaseRepo propertyPurchaseRepo;

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

            if (expense.getPaymentType().equals(PaymentType.CHEQUE) &&
                    expense.getPaymentStatus() != null && expense.getPaymentStatus().equals(PaymentStatus.PAID)) {
                journalEntry.setReferenceType("EXPENSE_CHEQUE_PAYMENT");
                journalEntry.setDescription("PDC Clearing: Vendor# = " + expense.getVendorName() +
                        (expense.getProjectName() != null ? " - Project: " + expense.getProjectName() : ""));
            } else {
                journalEntry.setDescription("Expense: " + expense.getExpenseTitle() +
                        (expense.getProjectName() != null ? " - Project: " + expense.getProjectName() : ""));
            }
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for Expense ID: {}", journalEntry.getId(), expense.getId());

            // Find or create Chart of Account for Bank/Cash Account
            ChartOfAccount bankAccount = findBankAccount(organizationAccount.getId(), expense.getOrganizationId());
            log.info("Bank Account COA ID: {} - {}", bankAccount.getId(), bankAccount.getName());


            if (expense.getPaymentType().equals(PaymentType.CHEQUE)) {

                // when cheque is cleared, we can record the payment directly to bank account. But if it's not cleared yet, we need to record the payment in a separate "Cheque Account" to reflect the pending nature of the transaction.
                if (expense.getPaymentStatus() != null && expense.getPaymentStatus().equals(PaymentStatus.PAID)) {


                    ChartOfAccount chequeAccount = journalUtilities.vendorPayable(expense.getOrganizationId());
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
                    ChartOfAccount accountsPayableAccount = journalUtilities.vendorPayable(expense.getOrganizationId());
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

                } else { // when cheque is created but not yet cleared, we record the payment in a separate "Cheque Account" until it's cleared. This is to reflect the pending nature of the transaction.
                    ChartOfAccount chequeAccount = journalUtilities.constructionInventory(expense.getOrganizationId());
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
                    ChartOfAccount accountsPayableAccount = journalUtilities.vendorPayable(expense.getOrganizationId());
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

            } else {
                // If amountPaid > 0: Debit Expense, Credit Bank
                if (expense.getAmountPaid() > 0 && expense.getCreditAmount() == 0) {
                    // Debit: Expense Account

                    ChartOfAccount debitAccount =
                            expense.getExpenseType() != ExpenseType.CONSTRUCTION
                                    ? journalUtilities.findChartOfAccount(expense, loggedInUser)
                                    : journalUtilities.constructionInventory(expense.getOrganizationId());

                    if (debitAccount == null) {
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

                    ChartOfAccount constructionInventoryAccount = journalUtilities.
                            constructionInventory(organizationAccount.getOrganizationId());

                    // Find or create Accounts Payable account for vendor
                    ChartOfAccount accountsPayableAccount = journalUtilities.vendorPayable(expense.getOrganizationId());
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
            ChartOfAccount accountsPayableAccount = journalUtilities.vendorPayable(expense.getOrganizationId());

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
            journalEntry.setVendorId(vendorAccount.getId());
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
                    .vendorPayable(vendorAccount.getOrganizationId());

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

            ChartOfAccount customerReceivableAccount = journalUtilities.customerReceivable(booking.getOrganizationId());

            ChartOfAccount revenueAccount = journalUtilities.bookingRevenue(booking.getOrganizationId());

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
     * <p>
     * DR Bank / Cash
     * CR Booking Liability
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
            ChartOfAccount customerReceivableAccount = journalUtilities.customerReceivable(organizationId);

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
                ChartOfAccount undepositedAccount = journalUtilities.undepositedFunds(organizationId);

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

        if (entry.getTransactionCategory().equals(TransactionCategory.CUSTOMER_PAYMENT)) {
            return;
        }

        if (entry.getTransactionType() == TransactionType.CREDIT) {


            switch (entry.getTransactionCategory()) {

                case REFUND:
                    controlAccount = journalUtilities.customerRefund(organizationId).getName();
                    break;

                case WITHDRAWL:
                    controlAccount = journalUtilities.generalExpense(organizationId).getName();
                    break;

                case CONSTRUCTION:
                    controlAccount = journalUtilities.constructionExpense(organizationId).getName();
                    break;

                case ADJUSTMENT:
                    controlAccount = journalUtilities.adjustmentExpense(organizationId).getName();
                    break;

                case MISCALLENOUS:
                    controlAccount = journalUtilities.miscellaneousExpense(organizationId).getName();
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

        } else if (entry.getTransactionType() == TransactionType.DEBIT) {

            switch (entry.getTransactionCategory()) {

                case SCRAP_SALE:
                    controlAccount = journalUtilities.scrapIncomeAccount(organizationId).getName();
                    break;

                case OTHER, MISCALLENOUS:
                    controlAccount = journalUtilities.miscellaneousExpense(organizationId).getName();
                    break;

                case ADJUSTMENT:
                    controlAccount = journalUtilities.adjustmentExpense(organizationId).getName();
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


        try{
            ChartOfAccount companyAccount = findBankAccount(organizationAccountDetail.getOrganizationAcctId(), organizationId);


            ChartOfAccount controlAccount = journalUtilities.getChartOfAccount(organizationId,
                    controlAccountName);

    /*
     Step 2: Create Journal Entry (HEADER)
     */

            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setReferenceType(organizationAccountDetail.getTransactionCategory().name());
            journalEntry.setOrganizationAccountId(organizationAccountDetail.getOrganizationAcctId());
            journalEntry.setDescription(
                    "Adjustment in Account#: "
                            + organizationAccountDetail.getOrganizationAcctId()
                            + (controlAccountType.equals(TransactionType.DEBIT)
                            ? " Increased by "
                            : " Decreased by ")
                            + organizationAccountDetail.getAmount()
            );
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry.setProjectId(organizationAccountDetail.getProjectId());
            journalEntry.setUnitId(0L);
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

        }  catch (Exception e) {
        log.error("Failed Account Adjustment Entry for Booking {}: {}", organizationAccountDetail.getId(), e.getMessage(), e);
        throw new RuntimeException("Failed to create booking update journal entry: " + e.getMessage(), e);
    }



    }

    /**
     * Journal Entry for Booking Price Update.
     * <p>
     * If new price > old price (price increase):
     * DR Customer Receivable   (difference)
     * CR Booking Revenue       (difference)
     * <p>
     * If new price < old price (price decrease):
     * DR Booking Revenue       (difference)
     * CR Customer Receivable   (difference)
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
                    journalUtilities.customerReceivable(booking.getOrganizationId());

            ChartOfAccount bookingRevenueAccount =
                    journalUtilities.bookingRevenue(booking.getOrganizationId());

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
                    journalUtilities.customerReceivable(organizationId);

            ChartOfAccount bookingRevenue =
                    journalUtilities.bookingRevenue(organizationId);

            ChartOfAccount cancellationRevenue =
                    journalUtilities.cancellationRevenue(organizationId);

            ChartOfAccount refundPayable =
                    journalUtilities.customerRefund(organizationId);

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
                journalUtilities.customerRefund(organizationId);

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


        ChartOfAccount fromAccount = findBankAccount(request.getFromAccountId(), organizationId);
        ChartOfAccount toAccount = findBankAccount(request.getToAccountId(), organizationId);


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
     * DR Bank/Cash
     * CR Undeposited Funds (Customer Receivable)
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
                    journalUtilities.undepositedFunds(organizationId);

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

    @Transactional
    public void updateVendorPaymentJournalEntry(VendorAccount vendorAccount,
                                                OrganizationAccount organizationAccount,
                                                double oldPaymentAmount,
                                                double newPaymentAmount,
                                                String loggedInUser) {
        try {
            double difference = newPaymentAmount - oldPaymentAmount;
            if (Math.abs(difference) < 0.01) {
                log.info("Vendor payment amount unchanged for Vendor ID: {}, skipping journal entry.", vendorAccount.getId());
                return;
            }

            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // Create Journal Entry header
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(vendorAccount.getOrganizationId());
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("VENDOR_PAYMENT_UPDATE");
            journalEntry.setExpenseId(0L);
            journalEntry.setVendorId(vendorAccount.getId());
            journalEntry.setOrganizationAccountId(organizationAccount.getId());
            journalEntry.setDescription("Vendor Payment Update: " +
                    " - Adjusting payment for " + vendorAccount.getName() +
                    " | Old: " + oldPaymentAmount + " | New: " + newPaymentAmount);
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for Vendor Payment Update, Vendor ID: {}",
                    journalEntry.getId(), vendorAccount.getId());

            // Get Bank account
            ChartOfAccount bankAccount = findBankAccount(organizationAccount.getId(), vendorAccount.getOrganizationId());
            if (bankAccount == null) throw new RuntimeException("Bank account not found");

            // Get Vendor Payable (AP) account
            ChartOfAccount accountsPayableAccount = journalUtilities
                    .vendorPayable(vendorAccount.getOrganizationId());

            double absDifference = Math.abs(difference);

            if (difference > 0) {
                // Increasing payment: Debit AP more, Credit Bank more
                JournalDetailEntry debitAP = new JournalDetailEntry();
                debitAP.setJournalEntryId(journalEntry.getId());
                debitAP.setChartOfAccountId(accountsPayableAccount.getId());
                debitAP.setDebitAmount(absDifference);
                debitAP.setCreditAmount(0.0);
                debitAP.setDescription("Additional payment to vendor: " + vendorAccount.getName());
                detailEntries.add(debitAP);
                totalDebit += absDifference;

                JournalDetailEntry creditBank = new JournalDetailEntry();
                creditBank.setJournalEntryId(journalEntry.getId());
                creditBank.setChartOfAccountId(bankAccount.getId());
                creditBank.setDebitAmount(0.0);
                creditBank.setCreditAmount(absDifference);
                creditBank.setDescription("Additional payment from: " + organizationAccount.getName());
                detailEntries.add(creditBank);
                totalCredit += absDifference;
            } else {
                // Decreasing payment: Credit AP more, Debit Bank more (reverse)
                JournalDetailEntry creditAP = new JournalDetailEntry();
                creditAP.setJournalEntryId(journalEntry.getId());
                creditAP.setChartOfAccountId(accountsPayableAccount.getId());
                creditAP.setDebitAmount(0.0);
                creditAP.setCreditAmount(absDifference);
                creditAP.setDescription("Reduced payment to vendor: " + vendorAccount.getName());
                detailEntries.add(creditAP);
                totalCredit += absDifference;

                JournalDetailEntry debitBank = new JournalDetailEntry();
                debitBank.setJournalEntryId(journalEntry.getId());
                debitBank.setChartOfAccountId(bankAccount.getId());
                debitBank.setDebitAmount(absDifference);
                debitBank.setCreditAmount(0.0);
                debitBank.setDescription("Reduced payment from: " + organizationAccount.getName());
                detailEntries.add(debitBank);
                totalDebit += absDifference;
            }

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

            log.info("Vendor Payment Update Journal Entry {} completed. Difference: {}, Direction: {}",
                    journalEntry.getId(), absDifference, difference > 0 ? "INCREASE" : "DECREASE");

        } catch (Exception e) {
            log.error("Failed to update vendor payment journal entry for vendor {}: {}", vendorAccount.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to update vendor payment journal entry: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void createJournalEntryForProjectAcquisition(Project project, OrganizationAccount organizationAccount, String loggedInUser) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // Create Journal Entry header
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(project.getOrganizationId());
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("PROJECT_ACQUISITION");
            journalEntry.setProjectId(project.getProjectId());
            journalEntry.setOrganizationAccountId(organizationAccount.getId());
            journalEntry.setDescription("Project acquisition: " + project.getName() + " - Purchase cost: " + project.getTotalAmount());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for Project Acquisition, Project ID: {}", journalEntry.getId(), project.getProjectId());

            // Find or create Chart of Account for Bank/Cash Account
            ChartOfAccount bankAccount = findBankAccount(organizationAccount.getId(), project.getOrganizationId());
            if (bankAccount == null) {
                throw new RuntimeException("Bank account not found for organization account ID: " + organizationAccount.getId());
            }

            // Debit: Construction Inventory (asset increase)
            ChartOfAccount constructionInventoryAccount = journalUtilities.constructionInventory(project.getOrganizationId());
            JournalDetailEntry debitEntry = new JournalDetailEntry();
            debitEntry.setJournalEntryId(journalEntry.getId());
            debitEntry.setChartOfAccountId(constructionInventoryAccount.getId());
            debitEntry.setDebitAmount(project.getTotalAmount());
            debitEntry.setCreditAmount(0.0);
            debitEntry.setDescription("Project acquisition cost for: " + project.getName());
            detailEntries.add(debitEntry);
            totalDebit += project.getTotalAmount();

            // Credit: Bank Account (asset decrease)
            JournalDetailEntry creditEntry = new JournalDetailEntry();
            creditEntry.setJournalEntryId(journalEntry.getId());
            creditEntry.setChartOfAccountId(bankAccount.getId());
            creditEntry.setDebitAmount(0.0);
            creditEntry.setCreditAmount(project.getTotalAmount());
            creditEntry.setDescription("Payment from bank for project: " + project.getName());
            detailEntries.add(creditEntry);
            totalCredit += project.getTotalAmount();

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

            log.info("Journal Entry {} for Project Acquisition completed. Total Debit: {}, Total Credit: {}",
                    journalEntry.getId(), totalDebit, totalCredit);

        } catch (Exception e) {
            log.error("Failed to create journal entry for project acquisition {}: {}", project.getProjectId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create journal entry: " + e.getMessage(), e);
        }
    }

    /**
     * Property purchase journal entry.
     *
     * DR Property / Land Inventory
     * CR Property Seller Payable
     */
    @Transactional
    public void createJournalEntryForPropertyPurchase(PropertyPurchase purchase, String loggedInUser) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> entries = new ArrayList<>();

            if (purchase.getTotalAmount() <= 0) {
                throw new RuntimeException("Property purchase total amount is invalid");
            }

            ChartOfAccount inventoryAccount =
                    journalUtilities.standAlonePropertyInventory(purchase.getOrganizationId());
            ChartOfAccount sellerPayable =
                    journalUtilities.propertySellerPayable(purchase.getOrganizationId());

            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(purchase.getOrganizationId());
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("PROPERTY_PURCHASE");
            journalEntry.setAdditionalReferenceId(purchase.getId());
            journalEntry.setDescription("Property purchase created. Purchase ID: " + purchase.getId());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            // DR Inventory
            entries.add(buildEntry(journalEntry.getId(), inventoryAccount.getId(), purchase.getTotalAmount(), 0));
            totalDebit += purchase.getTotalAmount();

            // CR Seller Payable
            entries.add(buildEntry(journalEntry.getId(), sellerPayable.getId(), 0, purchase.getTotalAmount()));
            totalCredit += purchase.getTotalAmount();

            validateAndSave(entries, totalDebit, totalCredit);
        } catch (Exception e) {
            log.error("Failed property purchase journal for purchase {}: {}",
                    purchase != null ? purchase.getId() : null, e.getMessage(), e);
            throw new RuntimeException("Failed to create property purchase journal entry: " + e.getMessage(), e);
        }
    }

    /**
     * Property payment journal entry.
     *
     * DR Property Seller Payable
     * CR Bank/Cash account
     */
    @Transactional
    public void createJournalEntryForPropertyPayment(
            PropertyPurchase purchase,
            PropertyPayment payment,
            OrganizationAccount organizationAccount,
            String loggedInUser
    ) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> entries = new ArrayList<>();

            if (payment.getAmount() <= 0) {
                throw new RuntimeException("Property payment amount is invalid");
            }

            ChartOfAccount sellerPayable =
                    journalUtilities.propertySellerPayable(purchase.getOrganizationId());

            ChartOfAccount bankAccount =
                    findBankAccount(organizationAccount.getId(), purchase.getOrganizationId());

            if (bankAccount == null) {
                throw new RuntimeException("Bank account COA not found for organization account ID: " + organizationAccount.getId());
            }

            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(purchase.getOrganizationId());
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("PROPERTY_PAYMENT");
            journalEntry.setAdditionalReferenceId(payment.getId());
            journalEntry.setOrganizationAccountId(organizationAccount.getId());
            journalEntry.setDescription("Property payment against Purchase ID: " + purchase.getId() + " | Payment ID: " + payment.getId());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            // DR Seller Payable
            entries.add(buildEntry(journalEntry.getId(), sellerPayable.getId(), payment.getAmount(), 0));
            totalDebit += payment.getAmount();

            // CR Bank/Cash
            entries.add(buildEntry(journalEntry.getId(), bankAccount.getId(), 0, payment.getAmount()));
            totalCredit += payment.getAmount();

            validateAndSave(entries, totalDebit, totalCredit);
        } catch (Exception e) {
            log.error("Failed property payment journal for purchase {} payment {}: {}",
                    purchase != null ? purchase.getId() : null,
                    payment != null ? payment.getId() : null,
                    e.getMessage(), e);
            throw new RuntimeException("Failed to create property payment journal entry: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void createJournalEntryForGrn(Grn grn,
                                         Map<Long, PurchaseOrderItem> poItemMap,
                                         List<GrnItems> grnItemsList,
                                         String loggedInUser) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            double totalAmount = 0.0;

            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // ===========================
            // 1️⃣ Calculate Total GRN Amount
            // ===========================
            for (GrnItems item : grnItemsList) {
                PurchaseOrderItem poItem = poItemMap.get(item.getPoItemId());
                double rate = poItem.getRate();
                totalAmount += item.getQuantityReceived() * rate;
            }

            if (totalAmount <= 0) return;

            // ===========================
            // 2️⃣ Create Journal Entry Header
            // ===========================
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(grn.getOrgId());
            journalEntry.setCreatedDate(grn.getCreatedDate() != null ? grn.getCreatedDate() : LocalDateTime.now());
            journalEntry.setReferenceType("GRN");
            journalEntry.setAdditionalReferenceId(grn.getId());
            journalEntry.setVendorId(grn.getVendorId());
            journalEntry.setProjectId(grn.getProjectId());
            journalEntry.setDescription("GRN: " + grn.getGrnNumber());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for GRN ID: {}", journalEntry.getId(), grn.getId());

            // ===========================
            // 3️⃣ Get COA Accounts
            // ===========================
            ChartOfAccount grnClearingAccount =
                    journalUtilities.grnClearing(grn.getOrgId());

            ChartOfAccount debitAccount;

            if (grn.getReceiptType() == ReceiptType.STOCK) {
                debitAccount = journalUtilities.stockInventory(grn.getOrgId());
            } else {
                debitAccount = journalUtilities.constructionInventory(grn.getOrgId());
            }

            // ===========================
            // 4️⃣ Debit Entry
            // ===========================
            JournalDetailEntry debitEntry = new JournalDetailEntry();
            debitEntry.setJournalEntryId(journalEntry.getId());
            debitEntry.setChartOfAccountId(debitAccount.getId());
            debitEntry.setDebitAmount(totalAmount);
            debitEntry.setCreditAmount(0.0);
            debitEntry.setDescription(
                    grn.getReceiptType() == ReceiptType.STOCK
                            ? "Inventory received via GRN: " + grn.getGrnNumber()
                            : "Direct consumption for project via GRN: " + grn.getGrnNumber()
            );

            detailEntries.add(debitEntry);
            totalDebit += totalAmount;

            // ===========================
            // 5️⃣ Credit Entry (GRN Clearing)
            // ===========================
            JournalDetailEntry creditEntry = new JournalDetailEntry();
            creditEntry.setJournalEntryId(journalEntry.getId());
            creditEntry.setChartOfAccountId(grnClearingAccount.getId());
            creditEntry.setDebitAmount(0.0);
            creditEntry.setCreditAmount(totalAmount);
            creditEntry.setDescription("GRN clearing liability for GRN: " + grn.getGrnNumber());

            detailEntries.add(creditEntry);
            totalCredit += totalAmount;

            // ===========================
            // 6️⃣ Validate Double Entry
            // ===========================
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException("Journal Entry imbalance! Debit: " + totalDebit + ", Credit: " + totalCredit);
            }

            // ===========================
            // 7️⃣ Save Entries
            // ===========================
            for (JournalDetailEntry entry : detailEntries) {
                journalDetailEntryRepository.save(entry);
            }

            log.info("GRN Journal Entry {} completed. Total: {}", journalEntry.getId(), totalAmount);

        } catch (Exception e) {
            log.error("Failed to create journal entry for GRN {}: {}", grn.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create GRN journal entry: " + e.getMessage(), e);
        }
    }


    @Transactional
    public void updateJournalEntryForGrn(
            Grn updatedGrn,
            ReceiptType oldReceiptType,
            List<GrnItems> oldGrnItems,
            List<GrnItems> newGrnItems,
            Map<Long, PurchaseOrderItem> poItemMap,
            String loggedInUser
    ) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;

            double oldTotalAmount = 0.0;
            double newTotalAmount = 0.0;

            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // ===========================
            // 1️⃣ Calculate Old GRN Amount
            // ===========================
            for (GrnItems item : oldGrnItems) {
                PurchaseOrderItem poItem = poItemMap.get(item.getPoItemId());

                if (poItem == null) {
                    throw new RuntimeException("PO item not found for old GRN item: " + item.getPoItemId());
                }

                double rate = poItem.getRate();
                oldTotalAmount += item.getQuantityReceived() * rate;
            }

            // ===========================
            // 2️⃣ Calculate New GRN Amount
            // ===========================
            for (GrnItems item : newGrnItems) {
                PurchaseOrderItem poItem = poItemMap.get(item.getPoItemId());

                if (poItem == null) {
                    throw new RuntimeException("PO item not found for new GRN item: " + item.getPoItemId());
                }

                double rate = poItem.getRate();
                newTotalAmount += item.getQuantityReceived() * rate;
            }

            if (oldTotalAmount <= 0 && newTotalAmount <= 0) {
                return;
            }

            // ===========================
            // 3️⃣ Create Journal Entry Header
            // ===========================
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(updatedGrn.getOrgId());
            journalEntry.setCreatedDate(LocalDateTime.now());
            journalEntry.setReferenceType("GRN_UPDATE");
            journalEntry.setAdditionalReferenceId(updatedGrn.getId());
            journalEntry.setVendorId(updatedGrn.getVendorId());
            journalEntry.setProjectId(updatedGrn.getProjectId());
            journalEntry.setDescription("GRN Updated: " + updatedGrn.getGrnNumber());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created GRN Update Journal Entry ID: {} for GRN ID: {}",
                    journalEntry.getId(), updatedGrn.getId());

            // ===========================
            // 4️⃣ Get COA Accounts
            // ===========================
            ChartOfAccount grnClearingAccount =
                    journalUtilities.grnClearing(updatedGrn.getOrgId());

            ChartOfAccount oldDebitAccount;

            if (oldReceiptType == ReceiptType.STOCK) {
                oldDebitAccount = journalUtilities.stockInventory(updatedGrn.getOrgId());
            } else if (oldReceiptType == ReceiptType.DIRECT) {
                oldDebitAccount = journalUtilities.constructionInventory(updatedGrn.getOrgId());
            } else {
                throw new RuntimeException("Invalid old GRN receipt type for accounting");
            }

            ChartOfAccount newDebitAccount;

            if (updatedGrn.getReceiptType() == ReceiptType.STOCK) {
                newDebitAccount = journalUtilities.stockInventory(updatedGrn.getOrgId());
            } else if (updatedGrn.getReceiptType() == ReceiptType.DIRECT) {
                newDebitAccount = journalUtilities.constructionInventory(updatedGrn.getOrgId());
            } else {
                throw new RuntimeException("Invalid new GRN receipt type for accounting");
            }

            // ===========================
            // 5️⃣ Reverse Old GRN Entry
            // Old original was:
            // Inventory Dr
            //      GRN Clearing Cr
            //
            // Reversal:
            // GRN Clearing Dr
            //      Inventory Cr
            // ===========================
            if (oldTotalAmount > 0) {

                JournalDetailEntry reverseDebitEntry = new JournalDetailEntry();
                reverseDebitEntry.setJournalEntryId(journalEntry.getId());
                reverseDebitEntry.setChartOfAccountId(grnClearingAccount.getId());
                reverseDebitEntry.setDebitAmount(oldTotalAmount);
                reverseDebitEntry.setCreditAmount(0.0);
                reverseDebitEntry.setDescription("Reverse old GRN clearing: " + updatedGrn.getGrnNumber());

                detailEntries.add(reverseDebitEntry);
                totalDebit += oldTotalAmount;

                JournalDetailEntry reverseCreditEntry = new JournalDetailEntry();
                reverseCreditEntry.setJournalEntryId(journalEntry.getId());
                reverseCreditEntry.setChartOfAccountId(oldDebitAccount.getId());
                reverseCreditEntry.setDebitAmount(0.0);
                reverseCreditEntry.setCreditAmount(oldTotalAmount);
                reverseCreditEntry.setDescription(
                        oldReceiptType == ReceiptType.STOCK
                                ? "Reverse old stock inventory for GRN: " + updatedGrn.getGrnNumber()
                                : "Reverse old construction inventory for GRN: " + updatedGrn.getGrnNumber()
                );

                detailEntries.add(reverseCreditEntry);
                totalCredit += oldTotalAmount;
            }

            // ===========================
            // 6️⃣ Post New Updated GRN Entry
            // New entry:
            // Inventory Dr
            //      GRN Clearing Cr
            // ===========================
            if (newTotalAmount > 0) {

                JournalDetailEntry newDebitEntry = new JournalDetailEntry();
                newDebitEntry.setJournalEntryId(journalEntry.getId());
                newDebitEntry.setChartOfAccountId(newDebitAccount.getId());
                newDebitEntry.setDebitAmount(newTotalAmount);
                newDebitEntry.setCreditAmount(0.0);
                newDebitEntry.setDescription(
                        updatedGrn.getReceiptType() == ReceiptType.STOCK
                                ? "Updated stock inventory via GRN: " + updatedGrn.getGrnNumber()
                                : "Updated direct consumption for project via GRN: " + updatedGrn.getGrnNumber()
                );

                detailEntries.add(newDebitEntry);
                totalDebit += newTotalAmount;

                JournalDetailEntry newCreditEntry = new JournalDetailEntry();
                newCreditEntry.setJournalEntryId(journalEntry.getId());
                newCreditEntry.setChartOfAccountId(grnClearingAccount.getId());
                newCreditEntry.setDebitAmount(0.0);
                newCreditEntry.setCreditAmount(newTotalAmount);
                newCreditEntry.setDescription("Updated GRN clearing liability for GRN: " + updatedGrn.getGrnNumber());

                detailEntries.add(newCreditEntry);
                totalCredit += newTotalAmount;
            }

            // ===========================
            // 7️⃣ Validate Double Entry
            // ===========================
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException("GRN update journal imbalance! Debit: "
                        + totalDebit + ", Credit: " + totalCredit);
            }

            // ===========================
            // 8️⃣ Save Entries
            // ===========================
            for (JournalDetailEntry entry : detailEntries) {
                journalDetailEntryRepository.save(entry);
            }

            log.info("GRN Update Journal Entry {} completed. Old Total: {}, New Total: {}",
                    journalEntry.getId(), oldTotalAmount, newTotalAmount);

        } catch (Exception e) {
            log.error("Failed to create update journal entry for GRN {}: {}",
                    updatedGrn.getId(), e.getMessage(), e);

            throw new RuntimeException("Failed to create GRN update journal entry: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void cancelJournalEntryForGrn(
            Grn grn,
            Map<Long, PurchaseOrderItem> poItemMap,
            List<GrnItems> grnItemsList,
            String loggedInUser
    ) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            double totalAmount = 0.0;

            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // ===========================
            // 1️⃣ Calculate GRN Amount To Reverse
            // ===========================
            for (GrnItems item : grnItemsList) {
                PurchaseOrderItem poItem = poItemMap.get(item.getPoItemId());

                if (poItem == null) {
                    throw new RuntimeException("PO Item not found for GRN Item PO Item ID: " + item.getPoItemId());
                }

                double rate = poItem.getRate();
                totalAmount += item.getQuantityReceived() * rate;
            }

            if (totalAmount <= 0) {
                return;
            }

            // ===========================
            // 2️⃣ Create Journal Entry Header
            // ===========================
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(grn.getOrgId());
            journalEntry.setCreatedDate(LocalDateTime.now());
            journalEntry.setReferenceType("GRN_CANCEL");
            journalEntry.setAdditionalReferenceId(grn.getId());
            journalEntry.setVendorId(grn.getVendorId());
            journalEntry.setProjectId(grn.getProjectId());
            journalEntry.setDescription("GRN Cancelled: " + grn.getGrnNumber());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created GRN Cancel Journal Entry ID: {} for GRN ID: {}",
                    journalEntry.getId(), grn.getId());

            // ===========================
            // 3️⃣ Get COA Accounts
            // ===========================
            ChartOfAccount grnClearingAccount =
                    journalUtilities.grnClearing(grn.getOrgId());

            ChartOfAccount creditAccount;

            if (grn.getReceiptType() == ReceiptType.STOCK) {
                creditAccount = journalUtilities.stockInventory(grn.getOrgId());
            } else if (grn.getReceiptType() == ReceiptType.DIRECT) {
                creditAccount = journalUtilities.constructionInventory(grn.getOrgId());
            } else {
                throw new RuntimeException("Invalid GRN receipt type for cancel accounting");
            }

            // ===========================
            // 4️⃣ Debit GRN Clearing
            // Original was:
            // Inventory Dr
            //      GRN Clearing Cr
            //
            // Cancel reversal:
            // GRN Clearing Dr
            //      Inventory Cr
            // ===========================
            JournalDetailEntry debitEntry = new JournalDetailEntry();
            debitEntry.setJournalEntryId(journalEntry.getId());
            debitEntry.setChartOfAccountId(grnClearingAccount.getId());
            debitEntry.setDebitAmount(totalAmount);
            debitEntry.setCreditAmount(0.0);
            debitEntry.setDescription("Reverse GRN clearing for cancelled GRN: " + grn.getGrnNumber());

            detailEntries.add(debitEntry);
            totalDebit += totalAmount;

            // ===========================
            // 5️⃣ Credit Inventory / Construction Inventory
            // ===========================
            JournalDetailEntry creditEntry = new JournalDetailEntry();
            creditEntry.setJournalEntryId(journalEntry.getId());
            creditEntry.setChartOfAccountId(creditAccount.getId());
            creditEntry.setDebitAmount(0.0);
            creditEntry.setCreditAmount(totalAmount);
            creditEntry.setDescription(
                    grn.getReceiptType() == ReceiptType.STOCK
                            ? "Reverse stock inventory for cancelled GRN: " + grn.getGrnNumber()
                            : "Reverse construction inventory for cancelled GRN: " + grn.getGrnNumber()
            );

            detailEntries.add(creditEntry);
            totalCredit += totalAmount;

            // ===========================
            // 6️⃣ Validate Double Entry
            // ===========================
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException(
                        "GRN cancel journal imbalance! Debit: " + totalDebit + ", Credit: " + totalCredit
                );
            }

            // ===========================
            // 7️⃣ Save Entries
            // ===========================
            for (JournalDetailEntry entry : detailEntries) {
                journalDetailEntryRepository.save(entry);
            }

            log.info("GRN Cancel Journal Entry {} completed. Total: {}",
                    journalEntry.getId(), totalAmount);

        } catch (Exception e) {
            log.error("Failed to create cancel journal entry for GRN {}: {}",
                    grn.getId(), e.getMessage(), e);

            throw new RuntimeException("Failed to create GRN cancel journal entry: " + e.getMessage(), e);
        }
    }


    /**
     * Property assignment journal entry for NEW_PROJECT creation.
     * This is an internal asset reclassification when a property is assigned to a project.
     *
     * DR Projects-Inventory (asset increase)
     * CR Property Land Inventory (asset decrease)
     */
    @Transactional
    public Map<String, Object> createPropertyAssignmentJournalEntry(
            long projectId,
            long propertyPurchaseId,
            double amount,
            String loggedInUser
    ) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> entries = new ArrayList<>();

            if (amount <= 0) {
                throw new RuntimeException("Property assignment amount is invalid");
            }

            // Get organization ID from property purchase
            Optional<PropertyPurchase> propertyPurchaseOpt = propertyPurchaseRepo.findById(propertyPurchaseId);
            if (propertyPurchaseOpt.isEmpty()) {
                throw new RuntimeException("Property purchase not found for ID: " + propertyPurchaseId);
            }
            PropertyPurchase propertyPurchase = propertyPurchaseOpt.get();
            long organizationId = propertyPurchase.getOrganizationId();

            ChartOfAccount projectsInventoryAccount =
                    journalUtilities.constructionInventory(organizationId);

            ChartOfAccount propertyLandInventoryAccount =
                    journalUtilities.standAlonePropertyInventory(organizationId);

            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(organizationId);
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("PROPERTY_ASSIGNMENT");
            journalEntry.setProjectId(projectId);
            journalEntry.setAdditionalReferenceId(propertyPurchaseId);
            journalEntry.setDescription("Property assignment to project. Project ID: " + projectId + " | Property Purchase ID: " + propertyPurchaseId);
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            // DR Projects-Inventory (asset increase)
            entries.add(buildEntry(journalEntry.getId(), projectsInventoryAccount.getId(), amount, 0));
            totalDebit += amount;

            // CR Property Land Inventory (asset decrease)
            entries.add(buildEntry(journalEntry.getId(), propertyLandInventoryAccount.getId(), 0, amount));
            totalCredit += amount;

            validateAndSave(entries, totalDebit, totalCredit);

            log.info("Property Assignment Journal Entry completed. Project ID: {}, Amount: {}", projectId, amount);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Property assignment journal entry created successfully");

        } catch (Exception e) {
            log.error("Failed property assignment journal for project {} property {}: {}",
                    projectId, propertyPurchaseId, e.getMessage(), e);
            throw new RuntimeException("Failed to create property assignment journal entry: " + e.getMessage(), e);
        }
    }


    @Transactional
    public void createJournalEntryForVendorInvoice(
            VendorInvoice invoice,
            List<VendorInvoiceItem> invoiceItems,
            String loggedInUser
    ) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            double totalAmount = 0.0;

            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // ===========================
            // 1️⃣ Calculate Invoice Amount
            // ===========================
            for (VendorInvoiceItem item : invoiceItems) {
                double amount = item.getAmount() != null
                        ? item.getAmount()
                        : item.getQuantity() * item.getRate();

                totalAmount += amount;
            }

            if (totalAmount <= 0) {
                return;
            }

            // ===========================
            // 2️⃣ Create Journal Entry Header
            // ===========================
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(invoice.getOrgId());
            journalEntry.setCreatedDate(invoice.getCreatedDate() != null ? invoice.getCreatedDate() : LocalDateTime.now());
            journalEntry.setReferenceType("VENDOR_INVOICE");
            journalEntry.setAdditionalReferenceId(invoice.getId());
            journalEntry.setAdditionalReferenceId(invoice.getGrnId());
            journalEntry.setVendorId(invoice.getVendorId());
            journalEntry.setProjectId(invoice.getProjectId());
            journalEntry.setDescription("Vendor Invoice: " + invoice.getInvoiceNumber());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for Vendor Invoice ID: {}",
                    journalEntry.getId(), invoice.getId());

            // ===========================
            // 3️⃣ Get COA Accounts
            // ===========================
            ChartOfAccount grnClearingAccount =
                    journalUtilities.grnClearing(invoice.getOrgId());

            ChartOfAccount vendorPayableAccount =
                    journalUtilities.vendorPayable(invoice.getOrgId());

            // ===========================
            // 4️⃣ Debit GRN Clearing
            // ===========================
            JournalDetailEntry debitEntry = new JournalDetailEntry();
            debitEntry.setJournalEntryId(journalEntry.getId());
            debitEntry.setChartOfAccountId(grnClearingAccount.getId());
            debitEntry.setDebitAmount(totalAmount);
            debitEntry.setCreditAmount(0.0);
            debitEntry.setDescription("GRN clearing transferred to vendor payable. Invoice: " + invoice.getInvoiceNumber());

            detailEntries.add(debitEntry);
            totalDebit += totalAmount;

            // ===========================
            // 5️⃣ Credit Vendor Payable
            // ===========================
            JournalDetailEntry creditEntry = new JournalDetailEntry();
            creditEntry.setJournalEntryId(journalEntry.getId());
            creditEntry.setChartOfAccountId(vendorPayableAccount.getId());
            creditEntry.setDebitAmount(0.0);
            creditEntry.setCreditAmount(totalAmount);
            creditEntry.setDescription("Vendor payable created. Invoice: " + invoice.getInvoiceNumber());

            detailEntries.add(creditEntry);
            totalCredit += totalAmount;

            // ===========================
            // 6️⃣ Validate Double Entry
            // ===========================
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException("Vendor invoice journal imbalance! Debit: "
                        + totalDebit + ", Credit: " + totalCredit);
            }

            // ===========================
            // 7️⃣ Save Entries
            // ===========================
            for (JournalDetailEntry entry : detailEntries) {
                journalDetailEntryRepository.save(entry);
            }

            log.info("Vendor Invoice Journal Entry {} completed. Total: {}",
                    journalEntry.getId(), totalAmount);

        } catch (Exception e) {
            log.error("Failed to create journal entry for Vendor Invoice {}: {}",
                    invoice.getId(), e.getMessage(), e);

            throw new RuntimeException("Failed to create vendor invoice journal entry: " + e.getMessage(), e);
        }
    }


    @Transactional
    public void updateJournalEntryForVendorInvoice(
            VendorInvoice invoice,
            List<VendorInvoiceItem> oldInvoiceItems,
            List<VendorInvoiceItem> newInvoiceItems,
            String loggedInUser
    ) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;

            double oldTotalAmount = 0.0;
            double newTotalAmount = 0.0;

            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // ===========================
            // 1️⃣ Calculate Old Invoice Amount
            // ===========================
            for (VendorInvoiceItem item : oldInvoiceItems) {
                double amount = item.getAmount() != null
                        ? item.getAmount()
                        : item.getQuantity() * item.getRate();

                oldTotalAmount += amount;
            }

            // ===========================
            // 2️⃣ Calculate New Invoice Amount
            // ===========================
            for (VendorInvoiceItem item : newInvoiceItems) {
                double amount = item.getAmount() != null
                        ? item.getAmount()
                        : item.getQuantity() * item.getRate();

                newTotalAmount += amount;
            }

            if (oldTotalAmount <= 0 && newTotalAmount <= 0) {
                return;
            }

            // ===========================
            // 3️⃣ Create Journal Entry Header
            // ===========================
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(invoice.getOrgId());
            journalEntry.setCreatedDate(LocalDateTime.now());
            journalEntry.setReferenceType("VENDOR_INVOICE_UPDATE");
            journalEntry.setAdditionalReferenceId(invoice.getId());
            journalEntry.setAdditionalReferenceId(invoice.getGrnId());
            journalEntry.setVendorId(invoice.getVendorId());
            journalEntry.setProjectId(invoice.getProjectId());
            journalEntry.setDescription("Vendor Invoice Updated: " + invoice.getInvoiceNumber());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Vendor Invoice Update Journal Entry ID: {} for Invoice ID: {}",
                    journalEntry.getId(), invoice.getId());

            // ===========================
            // 4️⃣ Get COA Accounts
            // ===========================
            ChartOfAccount grnClearingAccount =
                    journalUtilities.grnClearing(invoice.getOrgId());

            ChartOfAccount vendorPayableAccount =
                    journalUtilities.vendorPayable(invoice.getOrgId());

            // ===========================
            // 5️⃣ Reverse Old Invoice Entry
            // Original:
            // GRN Clearing Dr
            //      Vendor Payable Cr
            //
            // Reversal:
            // Vendor Payable Dr
            //      GRN Clearing Cr
            // ===========================
            if (oldTotalAmount > 0) {
                JournalDetailEntry reverseDebitEntry = new JournalDetailEntry();
                reverseDebitEntry.setJournalEntryId(journalEntry.getId());
                reverseDebitEntry.setChartOfAccountId(vendorPayableAccount.getId());
                reverseDebitEntry.setDebitAmount(oldTotalAmount);
                reverseDebitEntry.setCreditAmount(0.0);
                reverseDebitEntry.setDescription("Reverse old vendor payable. Invoice: " + invoice.getInvoiceNumber());

                detailEntries.add(reverseDebitEntry);
                totalDebit += oldTotalAmount;

                JournalDetailEntry reverseCreditEntry = new JournalDetailEntry();
                reverseCreditEntry.setJournalEntryId(journalEntry.getId());
                reverseCreditEntry.setChartOfAccountId(grnClearingAccount.getId());
                reverseCreditEntry.setDebitAmount(0.0);
                reverseCreditEntry.setCreditAmount(oldTotalAmount);
                reverseCreditEntry.setDescription("Reverse old GRN clearing. Invoice: " + invoice.getInvoiceNumber());

                detailEntries.add(reverseCreditEntry);
                totalCredit += oldTotalAmount;
            }

            // ===========================
            // 6️⃣ Post New Updated Invoice Entry
            // New:
            // GRN Clearing Dr
            //      Vendor Payable Cr
            // ===========================
            if (newTotalAmount > 0) {
                JournalDetailEntry newDebitEntry = new JournalDetailEntry();
                newDebitEntry.setJournalEntryId(journalEntry.getId());
                newDebitEntry.setChartOfAccountId(grnClearingAccount.getId());
                newDebitEntry.setDebitAmount(newTotalAmount);
                newDebitEntry.setCreditAmount(0.0);
                newDebitEntry.setDescription("Updated GRN clearing transferred. Invoice: " + invoice.getInvoiceNumber());

                detailEntries.add(newDebitEntry);
                totalDebit += newTotalAmount;

                JournalDetailEntry newCreditEntry = new JournalDetailEntry();
                newCreditEntry.setJournalEntryId(journalEntry.getId());
                newCreditEntry.setChartOfAccountId(vendorPayableAccount.getId());
                newCreditEntry.setDebitAmount(0.0);
                newCreditEntry.setCreditAmount(newTotalAmount);
                newCreditEntry.setDescription("Updated vendor payable. Invoice: " + invoice.getInvoiceNumber());

                detailEntries.add(newCreditEntry);
                totalCredit += newTotalAmount;
            }

            // ===========================
            // 7️⃣ Validate Double Entry
            // ===========================
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException("Vendor invoice update journal imbalance! Debit: "
                        + totalDebit + ", Credit: " + totalCredit);
            }

            // ===========================
            // 8️⃣ Save Entries
            // ===========================
            for (JournalDetailEntry entry : detailEntries) {
                journalDetailEntryRepository.save(entry);
            }

            log.info("Vendor Invoice Update Journal Entry {} completed. Old Total: {}, New Total: {}",
                    journalEntry.getId(), oldTotalAmount, newTotalAmount);

        } catch (Exception e) {
            log.error("Failed to create update journal entry for Vendor Invoice {}: {}",
                    invoice.getId(), e.getMessage(), e);

            throw new RuntimeException("Failed to create vendor invoice update journal entry: " + e.getMessage(), e);
        }
    }

//    vendor invoice payment

    @Transactional
    public void createJournalEntryForVendorPayment(
            VendorPaymentPO payment,
            Long organizationAccountId,
            String loggedInUser
    ) {
        try {

            double amount = payment.getAmount() != null ? payment.getAmount() : 0.0;
            if (amount <= 0) return;

            double totalDebit = 0;
            double totalCredit = 0;

            // ✅ Accounts
            ChartOfAccount vendorPayable =
                    journalUtilities.vendorPayable(payment.getOrgId());

            ChartOfAccount cashAccount =
                    chartOfAccountRepository.findById(organizationAccountId)
                            .orElseThrow(() -> new RuntimeException("Cash/Bank account not found"));

            // ✅ Journal Header
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(payment.getOrgId());
            journalEntry.setReferenceType("VENDOR_PAYMENT");
            journalEntry.setAdditionalReferenceId(payment.getId());
            journalEntry.setVendorId(payment.getVendorId());
            journalEntry.setProjectId(payment.getProjectId());
            journalEntry.setDescription("Vendor Payment: " + payment.getReferenceNumber());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            List<JournalDetailEntry> entries = new ArrayList<>();

            // ✅ DR Vendor Payable
            entries.add(buildEntry(
                    journalEntry.getId(),
                    vendorPayable.getId(),
                    amount,
                    0
            ));
            totalDebit += amount;

            // ✅ CR Cash / Bank
            entries.add(buildEntry(
                    journalEntry.getId(),
                    cashAccount.getId(),
                    0,
                    amount

            ));
            totalCredit += amount;

            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException("Vendor payment journal imbalance");
            }

            journalDetailEntryRepository.saveAll(entries);

        } catch (Exception e) {
            throw new RuntimeException("Vendor payment journal failed: " + e.getMessage(), e);
        }
    }


    @Transactional
    public void updateJournalEntryForVendorPayment(
            VendorPaymentPO existingPayment,
            double delta,
            Long organizationAccountId,
            String loggedInUser
    ) {
        try {

            if (delta == 0) return;

            double totalDebit = 0;
            double totalCredit = 0;

            ChartOfAccount vendorPayable =
                    journalUtilities.vendorPayable(existingPayment.getOrgId());

            ChartOfAccount cashAccount =
                    chartOfAccountRepository.findById(organizationAccountId)
                            .orElseThrow(() -> new RuntimeException("Cash/Bank account not found"));

            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(existingPayment.getOrgId());
            journalEntry.setReferenceType("VENDOR_PAYMENT_UPDATE");
            journalEntry.setAdditionalReferenceId(existingPayment.getId());
            journalEntry.setVendorId(existingPayment.getVendorId());
            journalEntry.setProjectId(existingPayment.getProjectId());
            journalEntry.setDescription("Vendor Payment Update: " + existingPayment.getReferenceNumber());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            List<JournalDetailEntry> entries = new ArrayList<>();

            if (delta > 0) {
                // ✅ Payment Increased

                entries.add(buildEntry(journalEntry.getId(), vendorPayable.getId(), delta, 0));
                entries.add(buildEntry(journalEntry.getId(), cashAccount.getId(), 0, delta));
                totalDebit += delta;
                totalCredit += delta;

            } else {
                double refund = -delta;

                // ✅ Payment Decreased (Refund case)

                entries.add(buildEntry(journalEntry.getId(), cashAccount.getId(), refund, 0));
                entries.add(buildEntry(journalEntry.getId(), vendorPayable.getId(), 0, refund));

                totalDebit += refund;
                totalCredit += refund;
            }

            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException("Vendor payment update imbalance");
            }

            journalDetailEntryRepository.saveAll(entries);

        } catch (Exception e) {
            throw new RuntimeException("Vendor payment update journal failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void cancelJournalEntryForVendorPayment(
            VendorPaymentPO payment,
            Long organizationAccountId,
            String loggedInUser
    ) {
        try {

            double amount = payment.getAmount();

            ChartOfAccount vendorPayable =
                    journalUtilities.vendorPayable(payment.getOrgId());

            ChartOfAccount cashAccount =
                    chartOfAccountRepository.findById(organizationAccountId)
                            .orElseThrow(() -> new RuntimeException("Cash/Bank account not found"));

            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(payment.getOrgId());
            journalEntry.setReferenceType("VENDOR_PAYMENT_CANCEL");
            journalEntry.setAdditionalReferenceId(payment.getId());
            journalEntry.setVendorId(payment.getVendorId());
            journalEntry.setProjectId(payment.getProjectId());
            journalEntry.setDescription("Vendor Payment Cancel: " + payment.getReferenceNumber());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            List<JournalDetailEntry> entries = new ArrayList<>();

            // ✅ Reverse payment
            entries.add(buildEntry(journalEntry.getId(), cashAccount.getId(), amount, 0));
            entries.add(buildEntry(journalEntry.getId(), vendorPayable.getId(), 0, amount));

            journalDetailEntryRepository.saveAll(entries);

        } catch (Exception e) {
            throw new RuntimeException("Vendor payment cancel journal failed", e);
        }
    }

//    payroll management

    @Transactional
    public void createJournalEntryForSalarySlip(
            SalarySlip slip,
            String loggedInUser
    ) {
        try {

            double amount = slip.getNetSalary() != null
                    ? slip.getNetSalary().doubleValue()
                    : 0.0;

            if (amount <= 0) return;

            double totalDebit = 0;
            double totalCredit = 0;

            // ✅ Accounts
            ChartOfAccount expense =
                    journalUtilities.salaryExpense(slip.getOrganizationId());

            ChartOfAccount payable =
                    journalUtilities.salaryPayable(slip.getOrganizationId());

            // ✅ Journal Header
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(slip.getOrganizationId());
            journalEntry.setReferenceType("SALARY_SLIP");
            journalEntry.setAdditionalReferenceId(slip.getId());
            journalEntry.setEmployeeId(slip.getEmployeeId());
            journalEntry.setDescription(
                    "Salary Generated - " +
                            slip.getEmployeeName() +
                            " (" + slip.getSalaryMonth() + "/" + slip.getSalaryYear() + ")"
            );
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            List<JournalDetailEntry> entries = new ArrayList<>();

            // ✅ DR Salary Expense
            entries.add(buildEntry(
                    journalEntry.getId(),
                    expense.getId(),
                    amount,
                    0
            ));
            totalDebit += amount;

            // ✅ CR Salary Payable
            entries.add(buildEntry(
                    journalEntry.getId(),
                    payable.getId(),
                    0,
                    amount
            ));
            totalCredit += amount;

            // ✅ Balance Validation
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException("Salary slip journal imbalance");
            }

            journalDetailEntryRepository.saveAll(entries);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Salary slip journal failed: " + e.getMessage(),
                    e
            );
        }
    }


    @Transactional
    public void paySalaryJournalEntry(
            SalarySlip slip,
            Long orgAccountId,
            String loggedInUser
    ) {
        try {

            double amount = slip.getNetSalary() != null
                    ? slip.getNetSalary().doubleValue()
                    : 0.0;

            if (amount <= 0) return;

            double totalDebit = 0;
            double totalCredit = 0;

            // ✅ Accounts
            ChartOfAccount payable =
                    journalUtilities.salaryPayable(slip.getOrganizationId());

            ChartOfAccount cash =
                    chartOfAccountRepository.findById(orgAccountId)
                            .orElseThrow(() ->
                                    new RuntimeException("Cash/Bank account not found"));

            // ✅ Journal Header
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(slip.getOrganizationId());
            journalEntry.setReferenceType("SALARY_PAYMENT");
            journalEntry.setAdditionalReferenceId(slip.getId());
            journalEntry.setEmployeeId(slip.getEmployeeId());
            journalEntry.setDescription(
                    "Salary Paid - " +
                            slip.getEmployeeName() +
                            " (" + slip.getSalaryMonth() + "/" + slip.getSalaryYear() + ")"
            );
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);

            journalEntry = journalEntryRepository.save(journalEntry);

            List<JournalDetailEntry> entries = new ArrayList<>();

            // ✅ DR Salary Payable
            entries.add(buildEntry(
                    journalEntry.getId(),
                    payable.getId(),
                    amount,
                    0
            ));
            totalDebit += amount;

            // ✅ CR Cash / Bank
            entries.add(buildEntry(
                    journalEntry.getId(),
                    cash.getId(),
                    0,
                    amount
            ));
            totalCredit += amount;

            // ✅ Balance Validation
            if (Math.abs(totalDebit - totalCredit) > 0.01) {
                throw new RuntimeException("Salary payment journal imbalance");
            }

            journalDetailEntryRepository.saveAll(entries);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Salary payment journal failed: " + e.getMessage(),
                    e
            );
        }
    }

}
