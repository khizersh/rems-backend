package com.rem.backend.service;

import com.rem.backend.entity.account.AccountGroup;
import com.rem.backend.entity.account.ChartOfAccount;
import com.rem.backend.entity.account.JournalDetailEntry;
import com.rem.backend.entity.account.JournalEntry;
import com.rem.backend.entity.expense.Expense;
import com.rem.backend.entity.organization.OrganizationAccount;
import com.rem.backend.enums.AccountStatus;
import com.rem.backend.enums.JournalEntryStatus;
import com.rem.backend.repository.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalDetailEntryRepository journalDetailEntryRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final AccountGroupRepository accountGroupRepository;

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
            ChartOfAccount bankAccount = findBankAccount(organizationAccount, expense.getOrganizationId());
            log.info("Bank Account COA ID: {} - {}", bankAccount.getId(), bankAccount.getName());

            // Find or create Chart of Account for Expense Account
            ChartOfAccount expenseAccount = findOrCreateExpenseAccount(expense, loggedInUser);
            log.info("Expense Account COA ID: {} - {}", expenseAccount.getId(), expenseAccount.getName());

            // If amountPaid > 0: Debit Expense, Credit Bank
            if (expense.getAmountPaid() > 0) {
                if(expense.getExpenseCOAId() == 0) {
                    // Debit: Expense Account
                    JournalDetailEntry debitEntry = new JournalDetailEntry();
                    debitEntry.setJournalEntryId(journalEntry.getId());
                    debitEntry.setChartOfAccountId(expenseAccount.getId());
                    debitEntry.setDebitAmount(expense.getAmountPaid());
                    debitEntry.setCreditAmount(0.0);
                    debitEntry.setDescription("Expense: " + expense.getExpenseTitle());
                    detailEntries.add(debitEntry);
                    totalDebit += expense.getAmountPaid();

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
                else {
                    JournalDetailEntry debitInventoryEntry = new JournalDetailEntry();
                    debitInventoryEntry.setJournalEntryId(journalEntry.getId());
                    debitInventoryEntry.setChartOfAccountId(Construction Inventory.getId());
                    debitInventoryEntry.setDebitAmount(expense.getAmountPaid());
                    debitInventoryEntry.setCreditAmount(0.0);
                    debitInventoryEntry.setDescription("Expense: " + expense.getExpenseTitle());
                    detailEntries.add(debitInventoryEntry);
                    totalDebit += expense.getAmountPaid();


                    JournalDetailEntry creditVendorEntry = new JournalDetailEntry();
                    creditVendorEntry.setJournalEntryId(journalEntry.getId());
                    creditVendorEntry.setChartOfAccountId(expenseAccount.getId());
                    creditVendorEntry.setDebitAmount(0.0);
                    creditVendorEntry.setCreditAmount(expense.getAmountPaid());
                    creditVendorEntry.setDescription("Expense: " + expense.getExpenseTitle());
                    detailEntries.add(creditVendorEntry);
                    totalCredit += expense.getAmountPaid();

                    JournalDetailEntry debitVendorEntry = new JournalDetailEntry();
                    debitVendorEntry.setJournalEntryId(journalEntry.getId());
                    debitVendorEntry.setChartOfAccountId(expenseAccount.getId());
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

            // If creditAmount > 0: Debit Expense, Credit Accounts Payable (Vendor)
            if (expense.getCreditAmount() > 0 && expense.getVendorAccountId() != null) {
                // Find or create Accounts Payable account for vendor
                ChartOfAccount accountsPayableAccount = getVendorPayableControlAccount(expense.getOrganizationId());
                log.info("Accounts Payable COA ID: {} - {}", accountsPayableAccount.getId(), accountsPayableAccount.getName());

                // Debit: Expense Account
                JournalDetailEntry debitEntry = new JournalDetailEntry();
                debitEntry.setJournalEntryId(journalEntry.getId());
                debitEntry.setChartOfAccountId(expenseAccount.getId());
                debitEntry.setDebitAmount(expense.getCreditAmount());
                debitEntry.setCreditAmount(0.0);
                debitEntry.setDescription("Expense on credit: " + expense.getExpenseTitle());
                detailEntries.add(debitEntry);
                totalDebit += expense.getCreditAmount();

                // Credit: Accounts Payable (Vendor)
                JournalDetailEntry creditEntry = new JournalDetailEntry();
                creditEntry.setJournalEntryId(journalEntry.getId());
                creditEntry.setChartOfAccountId(accountsPayableAccount.getId());
                creditEntry.setDebitAmount(0.0);
                creditEntry.setCreditAmount(expense.getCreditAmount());
                creditEntry.setDescription("Payable to: " + expense.getVendorName());
                detailEntries.add(creditEntry);
                totalCredit += expense.getCreditAmount();
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
            ChartOfAccount bankAccount = findBankAccount(organizationAccount, expense.getOrganizationId());

            // Find or create Accounts Payable account for vendor
            ChartOfAccount accountsPayableAccount = getVendorPayableControlAccount(expense.getOrganizationId());

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

    /**
     * Find or create Chart of Account for Bank/Cash Account
     * This links OrganizationAccount to ChartOfAccount for proper ledger entries
     */
    private ChartOfAccount findBankAccount(OrganizationAccount organizationAccount, long organizationId) {
        // Find existing account by name and organization

        Optional<AccountGroup> accountGroup = accountGroupRepository.
                findByNameAndOrganization_OrganizationId("bank/cash", organizationId);

        Optional<ChartOfAccount> existingAccount = chartOfAccountRepository
                .findAllByOrganizationId(organizationId)
                .stream()
                .filter(coa -> coa.getOrganizationAccountId() == organizationAccount.getId()
                        && coa.getStatus() == AccountStatus.ACTIVE && coa.getAccountGroup().getId() ==
                        accountGroup.get().getId())
                .findFirst();

        return existingAccount.orElse(null);

    }

    /**
     * Find or create Chart of Account for Expense Account
     */
    private ChartOfAccount findOrCreateExpenseAccount(Expense expense, String loggedInUser) {

        // Try to find existing account
        Optional<ChartOfAccount> existingAccount = chartOfAccountRepository
                .findById(expense.getExpenseCOAId());

        return existingAccount.orElse(null);
    }

    private ChartOfAccount getVendorPayableControlAccount(long organizationId) {

        Optional<ChartOfAccount> controlAccount = chartOfAccountRepository
                .findAllByOrganizationId(organizationId)
                .stream()
                .filter(coa -> coa.getStatus() == AccountStatus.ACTIVE
                        && coa.getAccountGroup().getName().equalsIgnoreCase("Accounts Payable")
                        && coa.isSystemGenerated()) // Optional: mark system-generated control accounts
                .findFirst();

        return controlAccount.orElseThrow(() ->
                new RuntimeException("Vendor Payable control account not found for organization " + organizationId));
    }

}

