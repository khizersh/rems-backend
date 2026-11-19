package com.rem.backend.dto.projectcost;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ProjectCostSummaryResponse {
    private Long projectId;
    private BigDecimal purchasingAmount;
    private BigDecimal registrationAmount;
    private BigDecimal constructionAmount;
    private BigDecimal additionalAmount;
    private BigDecimal totalProjectCost;
    private BigDecimal totalPaidProjectCosts;
    private BigDecimal totalOutstandingProjectCosts;
    private BigDecimal pendingPdcAmount;
    private Map<String, BigDecimal> categoryBreakdown;
    private List<ProjectCostResponse> recentCosts;
}
