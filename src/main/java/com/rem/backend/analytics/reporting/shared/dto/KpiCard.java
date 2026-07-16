package com.rem.backend.analytics.reporting.shared.dto;

import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.enums.TrendDirection;
import com.rem.backend.analytics.reporting.shared.enums.ValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single KPI / summary tile for a dashboard.
 * <p>
 * {@code value} is the raw numeric value (so the frontend can re-format/locale it),
 * while {@code formattedValue} is a ready-to-display compact string (e.g. "1.23M").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiCard {
    private String key;
    private String label;
    private double value;
    private String formattedValue;
    /** Period-over-period change in percent. Null when no comparison baseline exists. */
    private Double changePercent;
    private TrendDirection trend;
    private ColorHint colorHint;
    private String icon;
    private ValueType type;
}
