package com.rem.backend.analytics.reporting.admin.query.projection;

/**
 * One month bucket of aggregated debit/credit (used for accounting-derived trends).
 */
public interface MonthlyAmountProjection {
    Integer getYr();

    Integer getMonthNo();

    Double getTotalDebit();

    Double getTotalCredit();
}
