package com.rem.backend.analytics.reporting.shared.dto;

import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A single named data series within a {@link ChartData}.
 * {@code data} is index-aligned with the chart's {@code labels}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartSeries {
    private String name;
    private List<Object> data;
    private ColorHint colorHint;
}
