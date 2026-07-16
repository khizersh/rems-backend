package com.rem.backend.analytics.reporting.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The single, dashboard-friendly envelope returned by every admin reporting endpoint.
 * <p>
 * Not every section is populated by every endpoint; unused sections are simply empty lists.
 * This keeps the frontend contract uniform: render whichever sections are present.
 *
 * <pre>
 * summaryCards  -&gt; KPI tiles (cards row)
 * breakdowns    -&gt; grouped summaries (assets/liabilities, status splits, ...)
 * charts        -&gt; trend/distribution charts
 * tables        -&gt; top-N lists and drill-down tables
 * alerts        -&gt; exceptions / warnings
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportingView {
    private String key;
    private String title;
    private String subtitle;
    private LocalDateTime generatedAt;
    private AppliedFilters filtersApplied;
    private List<KpiCard> summaryCards;
    private List<GroupedSummary> breakdowns;
    private List<ChartData> charts;
    private List<TableData> tables;
    private List<ExceptionItem> alerts;
}
