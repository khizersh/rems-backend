package com.rem.backend.analytics.download.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTotals {
    private Double totalDebit;
    private Double totalCredit;
    private Double totalOpening;
    private Double totalClosing;
    private Double totalIncome;
    private Double totalExpense;
    private Double netProfit;
    private Double totalAssets;
    private Double totalLiabilities;
    private Double totalEquity;
    private Double difference;
}
