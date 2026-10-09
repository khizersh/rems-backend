package com.rem.backend.analytics.reporting.hr.query.projection;

/**
 * A single {@code label -> amount} bucket (e.g. payroll cost by department, allowance totals
 * by allowance name).
 */
public interface LabeledAmountProjection {
    String getLabel();

    Double getAmount();
}
