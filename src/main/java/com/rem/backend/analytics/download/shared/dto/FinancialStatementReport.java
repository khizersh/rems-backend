package com.rem.backend.analytics.download.shared.dto;

import com.rem.backend.analytics.reporting.shared.dto.AppliedFilters;
import com.rem.backend.analytics.reporting.shared.dto.ExceptionItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialStatementReport {
    private String reportKey;
    private String title;
    private String subtitle;
    private String organizationName;
    private LocalDateTime generatedAt;
    private AppliedFilters filtersApplied;
    private List<ReportSection> sections;
    private ReportTotals totals;
    private List<ExceptionItem> validationAlerts;
}
