package com.rem.backend.analytics.reporting.hr.query.projection;

/**
 * One month bucket of a simple count (e.g. new hires, terminations, leave requests filed).
 */
public interface MonthlyCountProjection {
    Integer getYr();

    Integer getMonthNo();

    Long getCount();
}
