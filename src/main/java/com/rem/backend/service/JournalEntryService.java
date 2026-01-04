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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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
     * 
     * For cash payment (amountPaid > 0):
     *   Debit: Expense Account (amountPaid)
     *   Credit: Bank/Cash Account (amountPaid)
     * 
     * For credit purchase (creditAmount > 0):
     *   Debit: Expense Account (creditAmount)
     *   Credit: Accounts Payable - Vendor (creditAmount)
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
            journalEntry.setReferenceId(expense.getId());
            journalEntry.setDescription("Expense: " + expense.getExpenseTitle() + 
                    (expense.getProjectName() != null ? " - Project: " + expense.getProjectName() : ""));
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);
            
            log.info("Created Journal Entry ID: {} for Expense ID: {}", journalEntry.getId(), expense.getId());

            // Find or create Chart of Account for Bank/Cash Account
            ChartOfAccount bankAccount = findOrCreateBankAccount(organizationAccount, expense.getOrganizationId());
            log.info("Bank Account COA ID: {} - {}", bankAccount.getId(), bankAccount.getName());

            // Find or create Chart of Account for Expense Account
            ChartOfAccount expenseAccount = findOrCreateExpenseAccount(expense, loggedInUser);
            log.info("Expense Account COA ID: {} - {}", expenseAccount.getId(), expenseAccount.getName());

            // If amountPaid > 0: Debit Expense, Credit Bank
            if (expense.getAmountPaid() > 0) {
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

            // If creditAmount > 0: Debit Expense, Credit Accounts Payable (Vendor)
            if (expense.getCreditAmount() > 0 && expense.getVendorAccountId() != null) {
                // Find or create Accounts Payable account for vendor
                ChartOfAccount accountsPayableAccount = findOrCreateAccountsPayableAccount(expense, loggedInUser);
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
     * 
     * Double-entry bookkeeping:
     *   Debit: Accounts Payable - Vendor (reducing liability)
     *   Credit: Bank/Cash Account (reducing asset)
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
            journalEntry.setReferenceId(expense.getId());
            journalEntry.setDescription("Vendor Payment: " + expense.getExpenseTitle() + 
                    " - Paying debt to " + expense.getVendorName());
            journalEntry.setStatus(JournalEntryStatus.POSTED);
            journalEntry.setCreatedBy(loggedInUser);
            journalEntry = journalEntryRepository.save(journalEntry);

            log.info("Created Journal Entry ID: {} for Expense Payment, Expense ID: {}", 
                    journalEntry.getId(), expense.getId());

            // Find or create Chart of Account for Bank/Cash Account
            ChartOfAccount bankAccount = findOrCreateBankAccount(organizationAccount, expense.getOrganizationId());

            // Find or create Accounts Payable account for vendor
            ChartOfAccount accountsPayableAccount = findOrCreateAccountsPayableAccount(expense, loggedInUser);

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
    private ChartOfAccount findOrCreateBankAccount(OrganizationAccount organizationAccount, long organizationId) {
        // Find existing account by name and organization
        Optional<ChartOfAccount> existingAccount = chartOfAccountRepository
                .findAllByOrganizationId(organizationId)
                .stream()
                .filter(coa -> coa.getName().equalsIgnoreCase(organizationAccount.getName()) 
                        && coa.getStatus() == AccountStatus.ACTIVE)
                .findFirst();

        if (existingAccount.isPresent()) {
            return existingAccount.get();
        }

        // Find "Bank Account" or "Cash and Bank" account group (Asset type)
        var bankAccountGroup = accountGroupRepository.findAllByOrganizationId(organizationId)
                .stream()
                .filter(ag -> ag.getName().equalsIgnoreCase("Bank Account") || 
                             ag.getName().equalsIgnoreCase("Cash and Bank") ||
                             ag.getName().equalsIgnoreCase("Cash") ||
                             ag.getName().equalsIgnoreCase("Bank"))
                .findFirst()
                .orElseGet(() -> {
                    // If no bank account group exists, try to find any Asset group
                    return accountGroupRepository.findAllByOrganizationId(organizationId)
                            .stream()
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("No account groups found. Please create account groups first."));
                });

        // Create new Chart of Account for this bank
        ChartOfAccount chartOfAccount = new ChartOfAccount();
        chartOfAccount.setCode(generateAccountCode(organizationId, "BANK"));
        chartOfAccount.setName(organizationAccount.getName());
        chartOfAccount.setOrganizationId(organizationId);
        chartOfAccount.setAccountGroupId(bankAccountGroup.getId());
        chartOfAccount.setSystemGenerated(true);
        chartOfAccount.setStatus(AccountStatus.ACTIVE);
        
        ChartOfAccount savedAccount = chartOfAccountRepository.save(chartOfAccount);
        log.info("Created Bank Account in Chart of Accounts: {} - {}", savedAccount.getId(), savedAccount.getName());
        
        return savedAccount;
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

        // Find appropriate expense account group - try multiple common names
        var expenseAccountGroup = accountGroupRepository.findAllByOrganizationId(expense.getOrganizationId())
                .stream()
                .filter(ag -> {
                    String name = ag.getName().toLowerCase();
                    if (expense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {
                        return name.contains("construction") || name.contains("project") || name.contains("expense");
                    } else {
                        return name.contains("expense") || name.contains("marketing") || name.contains("miscellaneous");
                    }
                })
                .findFirst()
                .orElseGet(() -> {
                    // Fallback: use any available account group
                    return accountGroupRepository.findAllByOrganizationId(expense.getOrganizationId())
                            .stream()
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("No account groups found. Please create account groups first."));
                });

        // Create new Chart of Account
        ChartOfAccount chartOfAccount = new ChartOfAccount();
        chartOfAccount.setCode(generateAccountCode(expense.getOrganizationId(), "EXP"));
        chartOfAccount.setName(expenseAccountName);
        chartOfAccount.setOrganizationId(expense.getOrganizationId());
        chartOfAccount.setAccountGroupId(expenseAccountGroup.getId());
        if (expense.getProjectId() != null && expense.getProjectId() > 0) {
            chartOfAccount.setProjectId(expense.getProjectId());
        }
        chartOfAccount.setSystemGenerated(true);
        chartOfAccount.setStatus(AccountStatus.ACTIVE);
        
        ChartOfAccount savedAccount = chartOfAccountRepository.save(chartOfAccount);
        log.info("Created Expense Account in Chart of Accounts: {} - {}", savedAccount.getId(), savedAccount.getName());
        
        return savedAccount;
    }

    /**
     * Find or create Chart of Account for Accounts Payable (Vendor)
     * This creates a liability account for tracking vendor payables
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

        // Find liability/payable account group - try multiple common names
        var accountsPayableGroup = accountGroupRepository.findAllByOrganizationId(expense.getOrganizationId())
                .stream()
                .filter(ag -> {
                    String name = ag.getName().toLowerCase();
                    return name.contains("payable") || name.contains("liability") || 
                           name.contains("advance") || name.contains("creditor");
                })
                .findFirst()
                .orElseGet(() -> {
                    // Fallback: use any available account group
                    return accountGroupRepository.findAllByOrganizationId(expense.getOrganizationId())
                            .stream()
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("No account groups found. Please create account groups first."));
                });

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
        chartOfAccount.setSystemGenerated(true);
        chartOfAccount.setStatus(AccountStatus.ACTIVE);
        
        ChartOfAccount savedAccount = chartOfAccountRepository.save(chartOfAccount);
        log.info("Created Accounts Payable in Chart of Accounts: {} - {} for Vendor ID: {}", 
                savedAccount.getId(), savedAccount.getName(), expense.getVendorAccountId());
        
        return savedAccount;
    }

    /**
     * Generate a unique account code
     */
    private String generateAccountCode(long organizationId, String prefix) {
        long count = chartOfAccountRepository.findAllByOrganizationId(organizationId).size();
        return prefix + "-" + organizationId + "-" + (count + 1);
    }
}

