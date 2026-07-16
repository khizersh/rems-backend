package com.rem.backend.analytics.reporting.shared.dto;

import com.rem.backend.analytics.reporting.shared.enums.ChartType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Dashboard-ready chart payload. {@code labels} drive the x-axis (or pie legend) and
 * every {@link ChartSeries#getData()} is index-aligned with {@code labels}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartData {
    private String key;
    private String title;
    private ChartType type;
    private List<String> labels;
    private List<ChartSeries> series;
}
