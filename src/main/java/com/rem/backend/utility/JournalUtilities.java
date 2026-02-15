package com.rem.backend.utility;


import com.rem.backend.entity.account.ChartOfAccount;
import com.rem.backend.entity.expense.Expense;
import com.rem.backend.enums.AccountStatus;
import com.rem.backend.repository.ChartOfAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JournalUtilities {
    private final ChartOfAccountRepository chartOfAccountRepository;

    // Control Accounts
    public static final String SCRAP_INCOME_ACCOUNT = "Scrap Sales Income";
    public static final String EXPENSE_ACCOUNT = "General Expense";
    public static final String REFUND_ACCOUNT = "Customer Refund";
    public static final String ADJUSTMENT_ACCOUNT = "Account Adjustment";
    public static final String CONSTRUCTION_ACCOUNT = "Construction Expense";
    public static final String MISC_ACCOUNT = "Miscellaneous Expense";
    public static final String CONSTRUCTION_INVENTORY = "Construction Inventory";
    public static final String BOOKING_LIABILITY = "Booking Liability";
    public static final String VENDOR_PAYABLE = "Vendor Payable";



    public ChartOfAccount getChartOfAccount(long organizationId, String accountName) {
        return chartOfAccountRepository
                .findByOrganization_OrganizationIdAndNameIgnoreCaseAndStatusAndIsSystemGenerated(
                        organizationId,
                        accountName,
                        AccountStatus.ACTIVE,
                        true
                )
                .orElseThrow(() ->
                        new RuntimeException(accountName+ " account not found"));
    }


    public ChartOfAccount findChartOfAccount(Expense expense, String loggedInUser) {

        // Try to find existing account
        Optional<ChartOfAccount> existingAccount = chartOfAccountRepository
                .findById(expense.getExpenseCOAId());

        return existingAccount.orElse(null);
    }

}
