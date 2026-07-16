package com.rem.backend.analytics.reporting.shared.dto;

import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.enums.ValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A generic label/value pair used inside grouped summaries, legends and breakdowns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabeledValue {
    private String key;
    private String label;
    private double value;
    private String formattedValue;
    private ValueType type;
    private ColorHint colorHint;
}
