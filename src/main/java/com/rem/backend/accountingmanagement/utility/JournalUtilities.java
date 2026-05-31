package com.rem.backend.accountingmanagement.utility;


import com.rem.backend.accountingmanagement.entity.ChartOfAccount;
import com.rem.backend.entity.expense.Expense;
import com.rem.backend.enums.AccountStatus;
import com.rem.backend.accountingmanagement.repos.ChartOfAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JournalUtilities {
    private final ChartOfAccountRepository chartOfAccountRepository;

    // Control Accounts
//    public static final String SCRAP_INCOME_ACCOUNT = "Scrap Sales Income";
//    public static final String EXPENSE_ACCOUNT = "General Expense";
//    public static final String REFUND_ACCOUNT = "Customer Refund";
//    public static final String ADJUSTMENT_ACCOUNT = "Account Adjustment";
//    public static final String CONSTRUCTION_ACCOUNT = "Construction Expense";
//    public static final String MISC_ACCOUNT = "Miscellaneous Expense";
//    public static final String PROJECT_CONSTRUCTION_VALUE = "Construction Inventory";
//    public static final String STOCK_INVENTORY = "Stock-Inventory";
//    public static final String GRN_CLEARING = "GRN Clearing Account";
//    public static final String BOOKING_LIABILITY = "Booking Liability";
//    public static final String VENDOR_PAYABLE = "Vendor Payable";
//    public static final String PROPERTY_SELLER_PAYABLE = "Property-Seller-Payable";
//    public static final String CUSTOMER_RECEIVABLE = "Customer Receivable";
//    public static final String UNDEPOSITED_FUND_ACCOUNT = "Undeposited Funds";
//    public static final String BOOKING_REVENUE = "Booking Revenue";
//    public static final String CANCELLATION_REVENUE_ACCOUNT = "Booking Cancellation Revenue";
//    public static final String STANDALONE_PROPERTY_INVENTORY = "Standalone-Property-Inventory";






    // ✅ INVENTORY
    public static final String CONSTRUCTION_INVENTORY = "AST-INV-CONST-001";
    public static final String STOCK_INVENTORY = "AST-INV-STOCK-001";
    public static final String PROPERTY_INVENTORY = "AST-INV-PROPERTY-001";
    public static final String STANDALONE_PROPERTY_INVENTORY = "AST-STANDALONE-INV-PROPERTY-001";

    // ✅ PAYABLE
    public static final String VENDOR_PAYABLE = "LIA-VENDOR-001";
    public static final String BOOKING_LIABILITY = "LIA-BOOKING-001";
    public static final String CUSTOMER_REFUND = "LIA-REFUND-001";
    public static final String PROPERTY_SELLER_PAYABLE = "LIA-PROPERTY-SELLER-001";
    public static final String SALARY_PAYABLE = "LIA-SALARY-001";

    // ✅ RECEIVABLE
    public static final String CUSTOMER_RECEIVABLE = "AST-REC-CUSTOMER-001";

    // ✅ INCOME
    public static final String BOOKING_REVENUE = "INC-BOOKING-001";
    public static final String CANCELLATION_REVENUE = "INC-CANCEL-001";

    // ✅ EXPENSE
    public static final String WITHDRAWL_EQUITY = "EQT-WITHDRAWAL-001";
    public static final String CONSTRUCTION_EXPENSE = "EXP-CONST-001";
    public static final String MISCELLANEOUS_EXPENSE = "EXP-MISC-001";
    public static final String SALARY_EXPENSE = "EXP-SALARY-001";

    // ✅ SPECIAL
    public static final String UNDEPOSITED_FUNDS = "AST-UNDEP-001";
    public static final String GRN_CLEARING = "AST-GRN-CLR-001";
    public static final String ADJUSTMENT_EXPENSE = "EXP-ADJUSTMENT-001";
    public static final String SCRAP_INCOME_ACCOUNT = "INC-SCRAP-001";







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



    // ✅ COMMON FETCH
    public ChartOfAccount getByCode(Long orgId, String code) {
        return chartOfAccountRepository
                .findByOrganization_OrganizationIdAndCodeAndStatus(
                        orgId,
                        code,
                        AccountStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new RuntimeException("Account not found: " + code)
                );
    }

    // ✅ CLEAN FUNCTIONS

    public ChartOfAccount vendorPayable(Long orgId) {
        return getByCode(orgId, VENDOR_PAYABLE);
    }


    public ChartOfAccount bookingLiability(Long orgId) {
        return getByCode(orgId, BOOKING_LIABILITY);
    }

    public ChartOfAccount customerRefund(Long orgId) {
        return getByCode(orgId, CUSTOMER_REFUND);
    }

    public ChartOfAccount propertySellerPayable(Long orgId) {
        return getByCode(orgId, PROPERTY_SELLER_PAYABLE);
    }

    public ChartOfAccount customerReceivable(Long orgId) {
        return getByCode(orgId, CUSTOMER_RECEIVABLE);
    }


    public ChartOfAccount bookingRevenue(Long orgId) {
        return getByCode(orgId, BOOKING_REVENUE);
    }

    public ChartOfAccount cancellationRevenue(Long orgId) {
        return getByCode(orgId, CANCELLATION_REVENUE);
    }

    public ChartOfAccount withdrawlEquity(Long orgId) {
        return getByCode(orgId, WITHDRAWL_EQUITY);
    }

    public ChartOfAccount adjustmentExpense(Long orgId) {
        return getByCode(orgId, ADJUSTMENT_EXPENSE);
    }

    public ChartOfAccount miscellaneousExpense(Long orgId) {
        return getByCode(orgId, MISCELLANEOUS_EXPENSE);
    }

    public ChartOfAccount scrapIncomeAccount(Long orgId) {
        return getByCode(orgId, SCRAP_INCOME_ACCOUNT);
    }



    public ChartOfAccount constructionExpense(Long orgId) {
        return getByCode(orgId, CONSTRUCTION_EXPENSE);
    }

    public ChartOfAccount constructionInventory(Long orgId) {
        return getByCode(orgId, CONSTRUCTION_INVENTORY);
    }

    public ChartOfAccount standAlonePropertyInventory(Long orgId) {
        return getByCode(orgId, STANDALONE_PROPERTY_INVENTORY);
    }

    public ChartOfAccount stockInventory(Long orgId) {
        return getByCode(orgId, STOCK_INVENTORY);
    }

    public ChartOfAccount propertyInventory(Long orgId) {
        return getByCode(orgId, PROPERTY_INVENTORY);
    }

    public ChartOfAccount undepositedFunds(Long orgId) {
        return getByCode(orgId, UNDEPOSITED_FUNDS);
    }


    public ChartOfAccount grnClearing(Long orgId) {
        return getByCode(orgId, GRN_CLEARING);
    }

    public ChartOfAccount salaryExpense(Long orgId) {
        return getByCode(orgId, SALARY_EXPENSE);
    }


    public ChartOfAccount salaryPayable(Long orgId) {
        return getByCode(orgId, SALARY_PAYABLE);
    }



}
