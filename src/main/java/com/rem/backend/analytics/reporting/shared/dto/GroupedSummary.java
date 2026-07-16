package com.rem.backend.analytics.reporting.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A titled group of {@link LabeledValue}s with an optional roll-up total.
 * Useful for "Assets breakdown", "Booking status breakdown", etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupedSummary {
    private String key;
    private String label;
    private double total;
    private String formattedTotal;
    private List<LabeledValue> items;
}
