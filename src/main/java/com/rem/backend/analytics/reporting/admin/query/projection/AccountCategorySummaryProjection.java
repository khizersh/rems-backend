package com.rem.backend.analytics.reporting.admin.query.projection;

/**
 * Aggregated posted debit/credit totals grouped by account category (within a type).
 */
public interface AccountCategorySummaryProjection {
    String getAccountType();

    String getAccountCategory();

    Double getTotalDebit();

    Double getTotalCredit();
}
