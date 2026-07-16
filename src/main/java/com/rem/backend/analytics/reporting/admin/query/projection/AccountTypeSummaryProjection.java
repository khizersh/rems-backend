package com.rem.backend.analytics.reporting.admin.query.projection;

/**
 * Aggregated posted debit/credit totals grouped by account type (ASSET, LIABILITY, ...).
 * Backed by a native query; property names must match the SQL column aliases.
 */
public interface AccountTypeSummaryProjection {
    String getAccountType();

    Double getTotalDebit();

    Double getTotalCredit();
}
