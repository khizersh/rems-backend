package com.rem.backend.service;

import com.rem.backend.entity.account.ChartOfAccount;
import com.rem.backend.entity.account.JournalDetailEntry;
import com.rem.backend.entity.account.JournalEntry;
import com.rem.backend.entity.expense.Expense;
import com.rem.backend.entity.organization.OrganizationAccount;
import com.rem.backend.enums.AccountStatus;
import com.rem.backend.enums.JournalEntryStatus;
import com.rem.backend.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalDetailEntryRepository journalDetailEntryRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final AccountGroupRepository accountGroupRepository;

    /**
     * Create journal entry for expense creation with payment
     * Debit: Expense Account
     * Credit: Bank/Cash Account (from OrganizationAccount)
     * If creditAmount > 0: Also Debit Accounts Payable (Vendor)
     */
    @Transactional
    public void createJournalEntryForExpense(Expense expense, OrganizationAccount organizationAccount, String loggedInUser) {
        try {
            // Create Journal Entry
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(expense.getOrganizationId());
            journalEntry.setCreatedDate(expense.getCreatedDate());
            journalEntry.setReferenceType("EXPENSE");
            journalEntry.setReferenceId(expense.getId());
            journalEntry.setDescription("Expense: " + expense.getExpenseTitle() + 
                    (expense.getProjectName() != null ? " - " + expense.getProjectName() : ""));
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            // Find Chart of Account for Bank/Cash Account
            ChartOfAccount bankAccount = findBankAccount(organizationAccount);

            // Find or create Chart of Account for Expense Account
            ChartOfAccount expenseAccount = findOrCreateExpenseAccount(expense, loggedInUser);

            // If amountPaid > 0: Debit Expense, Credit Bank
            if (expense.getAmountPaid() > 0) {
                // Debit: Expense Account
                JournalDetailEntry debitEntry = new JournalDetailEntry();
                debitEntry.setJournalEntryId(journalEntry.getId());
                debitEntry.setChartOfAccountId(expenseAccount.getId());
                debitEntry.setDebitAmount(expense.getAmountPaid());
                debitEntry.setCreditAmount(0.0);
                debitEntry.setDescription("Expense payment: " + expense.getExpenseTitle());
                journalDetailEntryRepository.save(debitEntry);

                // Credit: Bank Account
                JournalDetailEntry creditEntry = new JournalDetailEntry();
                creditEntry.setJournalEntryId(journalEntry.getId());
                creditEntry.setChartOfAccountId(bankAccount.getId());
                creditEntry.setDebitAmount(0.0);
                creditEntry.setCreditAmount(expense.getAmountPaid());
                creditEntry.setDescription("Payment from " + organizationAccount.getName());
                journalDetailEntryRepository.save(creditEntry);
            }

            // If creditAmount > 0: Debit Expense, Credit Accounts Payable (Vendor)
            if (expense.getCreditAmount() > 0 && expense.getVendorAccountId() != null) {
                // Find or create Accounts Payable account for vendor
                ChartOfAccount accountsPayableAccount = findOrCreateAccountsPayableAccount(expense, loggedInUser);

                // Debit: Expense Account
                JournalDetailEntry debitEntry = new JournalDetailEntry();
                debitEntry.setJournalEntryId(journalEntry.getId());
                debitEntry.setChartOfAccountId(expenseAccount.getId());
                debitEntry.setDebitAmount(expense.getCreditAmount());
                debitEntry.setCreditAmount(0.0);
                debitEntry.setDescription("Expense on credit: " + expense.getExpenseTitle());
                journalDetailEntryRepository.save(debitEntry);

                // Credit: Accounts Payable (Vendor)
                JournalDetailEntry creditEntry = new JournalDetailEntry();
                creditEntry.setJournalEntryId(journalEntry.getId());
                creditEntry.setChartOfAccountId(accountsPayableAccount.getId());
                creditEntry.setDebitAmount(0.0);
                creditEntry.setCreditAmount(expense.getCreditAmount());
                creditEntry.setDescription("Credit from " + expense.getVendorName());
                journalDetailEntryRepository.save(creditEntry);
            }
        } catch (Exception e) {
            // Log error but don't fail the transaction
            e.printStackTrace();
        }
    }

    /**
     * Create journal entry for additional expense payment (ExpenseDetail)
     * Debit: Accounts Payable (Vendor)
     * Credit: Bank/Cash Account
     */
    @Transactional
    public void createJournalEntryForExpenseDetail(Expense expense, OrganizationAccount organizationAccount, 
                                                   double paymentAmount, String loggedInUser) {
        try {
            // Create Journal Entry
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setOrganizationId(expense.getOrganizationId());
            journalEntry.setCreatedDate(java.time.LocalDateTime.now());
            journalEntry.setReferenceType("EXPENSE_DETAIL");
            journalEntry.setReferenceId(expense.getId());
            journalEntry.setDescription("Payment against expense: " + expense.getExpenseTitle() + 
                    " - Paying debt of " + expense.getVendorName());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            // Find Chart of Account for Bank/Cash Account
            ChartOfAccount bankAccount = findBankAccount(organizationAccount);

            // Find or create Accounts Payable account for vendor
            ChartOfAccount accountsPayableAccount = findOrCreateAccountsPayableAccount(expense, loggedInUser);

            // Debit: Accounts Payable (Vendor)
            JournalDetailEntry debitEntry = new JournalDetailEntry();
            debitEntry.setJournalEntryId(journalEntry.getId());
            debitEntry.setChartOfAccountId(accountsPayableAccount.getId());
            debitEntry.setDebitAmount(paymentAmount);
            debitEntry.setCreditAmount(0.0);
            debitEntry.setDescription("Paying debt of " + expense.getVendorName());
            journalDetailEntryRepository.save(debitEntry);

            // Credit: Bank Account
            JournalDetailEntry creditEntry = new JournalDetailEntry();
            creditEntry.setJournalEntryId(journalEntry.getId());
            creditEntry.setChartOfAccountId(bankAccount.getId());
            creditEntry.setDebitAmount(0.0);
            creditEntry.setCreditAmount(paymentAmount);
            creditEntry.setDescription("Payment from " + organizationAccount.getName());
            journalDetailEntryRepository.save(creditEntry);
        } catch (Exception e) {
            // Log error but don't fail the transaction
            e.printStackTrace();
        }
    }

    /**
     * Find Chart of Account for Bank/Cash Account
     */
    private ChartOfAccount findBankAccount(OrganizationAccount organizationAccount) {
        // Find existing account by name and organization
        Optional<ChartOfAccount> existingAccount = chartOfAccountRepository
                .findAllByOrganizationId(organizationAccount.getOrganizationId())
                .stream()
                .filter(coa -> coa.getName().equalsIgnoreCase(organizationAccount.getName()) 
                        && coa.getStatus() == AccountStatus.ACTIVE)
                .findFirst();

        if (existingAccount.isPresent()) {
            return existingAccount.get();
        }

        throw new RuntimeException("Invalid bank account: Chart of Account not found for " + organizationAccount.getName() + ". Please create the account in Chart of Accounts first.");
    }

    /**
     * Find or create Chart of Account for Expense Account
     */
    private ChartOfAccount findOrCreateExpenseAccount(Expense expense, String loggedInUser) {
        String expenseAccountName;
        
        if (expense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {
            // For construction expenses, use expense type name or default
            expenseAccountName = expense.getExpenseTitle() != null ? expense.getExpenseTitle() : "Project Construction Cost";
        } else {
            expenseAccountName = "Miscellaneous Expense";
        }

        // Try to find existing account
        Optional<ChartOfAccount> existingAccount = chartOfAccountRepository
                .findAllByOrganizationId(expense.getOrganizationId())
                .stream()
                .filter(coa -> coa.getName().equalsIgnoreCase(expenseAccountName) 
                        && coa.getStatus() == AccountStatus.ACTIVE)
                .findFirst();

        if (existingAccount.isPresent()) {
            return existingAccount.get();
        }

        // Find appropriate expense account group
        String accountGroupName = expense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION) 
                ? "Project Construction Cost" : "Marketing Expense";
        
        var expenseAccountGroup = accountGroupRepository.findAllByOrganizationId(expense.getOrganizationId())
                .stream()
                .filter(ag -> ag.getName().equalsIgnoreCase(accountGroupName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(accountGroupName + " group not found. Please create account groups first."));

        // Create new Chart of Account
        ChartOfAccount chartOfAccount = new ChartOfAccount();
        chartOfAccount.setCode(generateAccountCode(expense.getOrganizationId(), "EXP"));
        chartOfAccount.setName(expenseAccountName);
        chartOfAccount.setOrganizationId(expense.getOrganizationId());
        chartOfAccount.setAccountGroupId(expenseAccountGroup.getId());
        if (expense.getProjectId() != null && expense.getProjectId() > 0) {
            chartOfAccount.setProjectId(expense.getProjectId());
        }
        chartOfAccount.setStatus(AccountStatus.ACTIVE);
        return chartOfAccountRepository.save(chartOfAccount);
    }

    /**
     * Find or create Chart of Account for Accounts Payable (Vendor)
     */
    private ChartOfAccount findOrCreateAccountsPayableAccount(Expense expense, String loggedInUser) {
        if (expense.getVendorAccountId() == null || expense.getVendorName() == null) {
            throw new RuntimeException("Vendor information is required for accounts payable");
        }

        String vendorAccountName = "Accounts Payable - " + expense.getVendorName();

        // Try to find existing account by vendor
        Optional<ChartOfAccount> existingAccount = chartOfAccountRepository
                .findAllByOrganizationIdAndVendorId(expense.getOrganizationId(), expense.getVendorAccountId())
                .stream()
                .filter(coa -> coa.getStatus() == AccountStatus.ACTIVE)
                .findFirst();

        if (existingAccount.isPresent()) {
            return existingAccount.get();
        }

        // Find "Customer Advances" or create a liability account group for vendors
        var accountsPayableGroup = accountGroupRepository.findAllByOrganizationId(expense.getOrganizationId())
                .stream()
                .filter(ag -> ag.getName().equalsIgnoreCase("Customer Advances") || 
                             ag.getName().equalsIgnoreCase("Accounts Payable"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Accounts Payable group not found. Please create account groups first."));

        // Create new Chart of Account
        ChartOfAccount chartOfAccount = new ChartOfAccount();
        chartOfAccount.setCode(generateAccountCode(expense.getOrganizationId(), "AP"));
        chartOfAccount.setName(vendorAccountName);
        chartOfAccount.setOrganizationId(expense.getOrganizationId());
        chartOfAccount.setAccountGroupId(accountsPayableGroup.getId());
        chartOfAccount.setVendorId(expense.getVendorAccountId());
        if (expense.getProjectId() != null && expense.getProjectId() > 0) {
            chartOfAccount.setProjectId(expense.getProjectId());
        }
        chartOfAccount.setStatus(AccountStatus.ACTIVE);
        return chartOfAccountRepository.save(chartOfAccount);
    }

    /**
     * Generate a unique account code
     */
    private String generateAccountCode(long organizationId, String prefix) {
        long count = chartOfAccountRepository.findAllByOrganizationId(organizationId).size();
        return prefix + "-" + organizationId + "-" + (count + 1);
    }
}

