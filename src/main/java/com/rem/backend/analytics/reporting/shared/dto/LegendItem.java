package com.rem.backend.analytics.reporting.shared.dto;

import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Legend entry for charts that need an explicit colour/label/value mapping (e.g. pie/donut).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegendItem {
    private String label;
    private Double value;
    private ColorHint colorHint;
}
