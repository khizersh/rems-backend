package com.rem.backend.analytics.reporting.hr.query.projection;

/**
 * One month bucket of a summed amount (e.g. leave days taken, payroll cost).
 */
public interface MonthlyAmountSumProjection {
    Integer getYr();

    Integer getMonthNo();

    Double getAmount();
}
