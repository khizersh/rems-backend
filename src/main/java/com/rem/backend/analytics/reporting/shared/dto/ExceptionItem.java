package com.rem.backend.analytics.reporting.shared.dto;

import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An alert / exception surfaced on the dashboard (overdue payments, anomalies, etc.).
 * {@code referenceId}/{@code referenceType} let the frontend deep-link to the source record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionItem {
    private String key;
    private Severity severity;
    private String title;
    private String description;
    private Double value;
    private String formattedValue;
    private Long referenceId;
    private String referenceType;
    private ColorHint colorHint;
}
