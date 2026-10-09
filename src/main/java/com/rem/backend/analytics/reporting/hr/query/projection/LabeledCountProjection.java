package com.rem.backend.analytics.reporting.hr.query.projection;

/**
 * A single {@code label -> count} bucket (e.g. headcount by department, by status, by type).
 */
public interface LabeledCountProjection {
    String getLabel();

    Long getCount();
}
