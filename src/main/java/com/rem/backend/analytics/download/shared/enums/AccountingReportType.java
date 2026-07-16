package com.rem.backend.analytics.download.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catalog of accounting reports available for download.
 */
@Getter
@RequiredArgsConstructor
public enum AccountingReportType {
    TRIAL_BALANCE("trial-balance", "Trial Balance", "Posted ledger balances with opening, movement and closing"),
    PROFIT_LOSS("profit-loss", "Profit & Loss Statement", "Income and expense for the selected period"),
    BALANCE_SHEET("balance-sheet", "Balance Sheet", "Assets, liabilities and equity as of a date"),
    GENERAL_LEDGER("general-ledger", "General Ledger", "Journal lines with running balance by account"),
    JOURNAL_REGISTER("journal-register", "Journal Register", "All posted journal vouchers in the period"),
    ACCOUNT_STATEMENT("account-statement", "Account Statement", "Transaction detail for a single chart-of-account");

    private final String key;
    private final String title;
    private final String description;
}
