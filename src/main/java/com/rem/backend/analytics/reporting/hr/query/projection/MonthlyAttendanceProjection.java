package com.rem.backend.analytics.reporting.hr.query.projection;

/**
 * One month bucket of attendance rate inputs: how many attendance rows were marked
 * present versus the total attendance rows logged for the org in that month.
 */
public interface MonthlyAttendanceProjection {
    Integer getYr();

    Integer getMonthNo();

    Long getPresentCount();

    Long getTotalCount();
}
