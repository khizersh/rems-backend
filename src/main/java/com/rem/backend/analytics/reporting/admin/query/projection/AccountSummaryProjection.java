package com.rem.backend.analytics.reporting.admin.query.projection;

/**
 * Aggregated posted debit/credit totals at the chart-of-account (code) level,
 * carrying the parent type/category for hierarchical drill-down.
 */
public interface AccountSummaryProjection {
    String getAccountType();

    String getAccountCategory();

    String getAccountCode();

    String getAccountName();

    Double getTotalDebit();

    Double getTotalCredit();
}
