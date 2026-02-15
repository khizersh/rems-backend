package com.rem.backend.service;

import com.rem.backend.accountmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.accountmanagement.enums.TransactionCategory;
import com.rem.backend.dto.orgAccount.TransferFundRequest;
import com.rem.backend.entity.account.AccountGroup;
import com.rem.backend.entity.account.ChartOfAccount;
import com.rem.backend.entity.account.JournalDetailEntry;
import com.rem.backend.entity.account.JournalEntry;
import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.entity.expense.Expense;
import com.rem.backend.accountmanagement.entity.OrganizationAccount;
import com.rem.backend.enums.AccountStatus;
import com.rem.backend.enums.ExpenseType;
import com.rem.backend.enums.JournalEntryStatus;
import com.rem.backend.enums.TransactionType;
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
    public void createVendorPaymentJournalEntry(Expense expense,
                                                OrganizationAccount organizationAccount,
                                                double paymentAmount,
                                                String loggedInUser) {
        try {
            double totalDebit = 0.0;
            double totalCredit = 0.0;
            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            // Create Journal Entry header
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(expense.getOrganizationId());
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("VENDOR_PAYMENT");
            journalEntry.setExpenseId(expense.getId());
            journalEntry.setOrganizationAccountId(organizationAccount.getId());
            journalEntry.setDescription("Vendor Payment: " + expense.getExpenseTitle() +
                    " - Paying " + expense.getVendorName());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for Vendor Payment, Expense ID: {}",
                    journalEntry.getId(), expense.getId());

            // Get Bank account
            ChartOfAccount bankAccount = findBankAccount(organizationAccount.getId(), expense.getOrganizationId());
            if (bankAccount == null) throw new RuntimeException("Bank account not found");

            // Get Vendor Payable (AP) account
            ChartOfAccount accountsPayableAccount = journalUtilities.getChartOfAccount(expense.getOrganizationId(), VENDOR_PAYABLE);

            // Debit AP (reduce liability)
            JournalDetailEntry debitAP = new JournalDetailEntry();
            debitAP.setJournalEntryId(journalEntry.getId());
            debitAP.setChartOfAccountId(accountsPayableAccount.getId());
            debitAP.setDebitAmount(paymentAmount);
            debitAP.setCreditAmount(0.0);
            debitAP.setDescription("Payment to vendor: " + expense.getVendorName());
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
            log.error("Failed to create vendor payment journal entry for expense {}: {}", expense.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create vendor payment journal entry: " + e.getMessage(), e);
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
            Long organizationId,
            CustomerPayment customerPayment,
            OrganizationAccountDetail organizationAccountDetail,
            Long unitId,
            Long bookingId,
            String loggedInUser
    ) {

        try {

            double totalDebit = 0.0;
            double totalCredit = 0.0;

            ChartOfAccount organizationAccount = findBankAccount(organizationAccountDetail.getOrganizationAcctId(),
                    organizationId);


            ChartOfAccount bookingLiabilityAccount = journalUtilities.getChartOfAccount(organizationId,
                    BOOKING_LIABILITY);



            List<JournalDetailEntry> detailEntries = new ArrayList<>();

            JournalEntry journalEntry = new JournalEntry();

            journalEntry.setOrganizationId(organizationId);

            journalEntry.setCreatedDate(customerPayment.getPaidDate());

            journalEntry.setReferenceType(TransactionCategory.CUSTOMER_PAYMENT.name());

            journalEntry.setExpenseId(null);

            journalEntry.setOrganizationAccountId(organizationAccount.getId());

            journalEntry.setUnitId(unitId);

            journalEntry.setProjectId(organizationAccountDetail.getProjectId());

            journalEntry.setBookingId(bookingId);

            journalEntry.setDescription(
                    "Customer Payment Received. Payment ID: "
                            + customerPayment.getId());

            journalEntry.setStatus(JournalEntryStatus.POSTED);

            journalEntry.setCreatedBy(loggedInUser);


            journalEntry = journalEntryRepository.save(journalEntry);


            log.info("Journal Entry created: {}", journalEntry.getId());


            JournalDetailEntry debitEntry = new JournalDetailEntry();

            debitEntry.setJournalEntryId(journalEntry.getId());

            debitEntry.setChartOfAccountId(organizationAccount.getId());

            debitEntry.setDebitAmount(customerPayment.getAmount());

            debitEntry.setCreditAmount(0.0);

            debitEntry.setDescription("Customer installment received");


            detailEntries.add(debitEntry);

            totalDebit += customerPayment.getAmount();



            JournalDetailEntry creditEntry = new JournalDetailEntry();

            creditEntry.setJournalEntryId(journalEntry.getId());

            creditEntry.setChartOfAccountId(bookingLiabilityAccount.getId());

            creditEntry.setDebitAmount(0.0);

            creditEntry.setCreditAmount(customerPayment.getAmount());

            creditEntry.setDescription("Customer booking liability");


            detailEntries.add(creditEntry);

            totalCredit += customerPayment.getAmount();


            if (Math.abs(totalDebit - totalCredit) > 0.01)
                throw new RuntimeException(
                        "Journal imbalance Debit: "
                                + totalDebit
                                + " Credit: "
                                + totalCredit);

            for (JournalDetailEntry entry : detailEntries) {

                journalDetailEntryRepository.save(entry);

                log.info(
                        "Journal Detail Saved COA: {} DR: {} CR: {}",
                        entry.getChartOfAccountId(),
                        entry.getDebitAmount(),
                        entry.getCreditAmount());
            }



            log.info(
                    "Customer Payment Journal Entry Completed Successfully: {}",
                    journalEntry.getId());


        }
        catch (Exception e) {

            log.error(
                    "Failed Customer Payment Journal Entry: {}",
                    e.getMessage(),
                    e);

            throw new RuntimeException(
                    "Customer Payment Journal failed: "
                            + e.getMessage(),
                    e);
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



}

