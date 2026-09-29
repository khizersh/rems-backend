package com.rem.backend.analytics.reporting.accountant.service;

import com.rem.backend.accountingmanagement.utility.JournalUtilities;
import com.rem.backend.analytics.reporting.admin.service.AdminFinancialReportService;
import com.rem.backend.analytics.reporting.shared.dto.ChartData;
import com.rem.backend.analytics.reporting.shared.dto.ExceptionItem;
import com.rem.backend.analytics.reporting.shared.dto.GroupedSummary;
import com.rem.backend.analytics.reporting.shared.dto.KpiCard;
import com.rem.backend.analytics.reporting.shared.dto.ReportingView;
import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.enums.Severity;
import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;
import com.rem.backend.analytics.reporting.shared.util.DateRange;
import com.rem.backend.analytics.reporting.shared.util.ReportingDateUtil;
import com.rem.backend.analytics.reporting.shared.util.ReportingFactory;
import com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Composes the Accountant finance dashboard: an accounting-first snapshot (assets, liabilities,
 * income, expense, net profit, receivables & payables) with category breakdowns, a revenue vs
 * collection trend and finance-focused alerts.
 * <p>
 * All money figures are delegated to {@link AdminFinancialReportService}, which derives them
 * from POSTED journals walked up the chart-of-account hierarchy (the single source of truth).
 */
@Service
@AllArgsConstructor
public class AccountantDashboardService {

    private final AdminFinancialReportService financialReportService;

    public ReportingView buildDashboard(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);

        double assets              = financialReportService.typeBalance(organizationId, range, "ASSET");
        double liabilities         = financialReportService.typeBalance(organizationId, range, "LIABILITY");
        double income              = financialReportService.typeBalance(organizationId, range, "INCOME");
        double expense             = financialReportService.typeBalance(organizationId, range, "EXPENSE");
        double equity              = financialReportService.typeBalance(organizationId, range, "EQUITY");
        double netProfit           = income - expense;
        double netWorth            = assets - liabilities;

        double receivables         = financialReportService.codeBalance(
                organizationId, range, JournalUtilities.CUSTOMER_RECEIVABLE);
        double currentLiabilities  = financialReportService.categoryBalance(organizationId, range, "LIABILITY", "CURRENT");
        double longTermLiabilities = financialReportService.categoryBalance(organizationId, range, "LIABILITY", "LONG TERM");

        double operatingIncome     = financialReportService.categoryBalance(organizationId, range, "INCOME", "OPERATING");
        double otherIncome         = financialReportService.categoryBalance(organizationId, range, "INCOME", "OTHER");
        double directExpense       = financialReportService.categoryBalance(organizationId, range, "EXPENSE", "DIRECT");
        double indirectExpense     = financialReportService.categoryBalance(organizationId, range, "EXPENSE", "INDIRECT");

        // --- KPI cards ----------------------------------------------------------
        List<KpiCard> cards = List.of(
                ReportingFactory.currencyCard("totalIncome",   "Total Income",   income,      "FaArrowTrendUp",      ColorHint.SUCCESS),
                ReportingFactory.currencyCard("totalExpense",  "Total Expense",  expense,     "FaReceipt",           ColorHint.DANGER),
                ReportingFactory.currencyCard("netProfit",     "Net Profit",     netProfit,   "FaScaleBalanced",
                        netProfit >= 0 ? ColorHint.SUCCESS : ColorHint.DANGER),
                ReportingFactory.currencyCard("totalReceivables", "Receivables", receivables, "FaHandHoldingDollar", ColorHint.WARNING),
                ReportingFactory.currencyCard("totalPayables", "Total Payables", liabilities, "FaFileInvoiceDollar", ColorHint.DANGER),
                ReportingFactory.currencyCard("totalAssets",   "Total Assets",   assets,      "FaCoins",             ColorHint.PRIMARY),
                ReportingFactory.currencyCard("totalEquity",   "Equity",         equity,      "FaBuildingColumns",   ColorHint.INFO),
                ReportingFactory.currencyCard("netWorth",      "Net Worth",      netWorth,    "FaVault",
                        netWorth >= 0 ? ColorHint.INFO : ColorHint.DANGER)
        );

        // --- Breakdowns ---------------------------------------------------------
        GroupedSummary financialSummary = GroupedSummary.builder()
                .key("financialSummary").label("Financial Summary")
                .total(ReportingFormatUtil.round2(netProfit))
                .formattedTotal(ReportingFormatUtil.compact(netProfit))
                .items(List.of(
                        ReportingFactory.currencyValue("assets",             "Assets",                assets,              ColorHint.PRIMARY),
                        ReportingFactory.currencyValue("currentLiabilities", "Current Liabilities",   currentLiabilities,  ColorHint.DANGER),
                        ReportingFactory.currencyValue("longTermLiabilities","Long-Term Liabilities", longTermLiabilities, ColorHint.WARNING),
                        ReportingFactory.currencyValue("income",             "Income",                income,              ColorHint.SUCCESS),
                        ReportingFactory.currencyValue("expense",            "Expense",               expense,             ColorHint.DANGER),
                        ReportingFactory.currencyValue("netProfit",          "Net Profit",            netProfit,
                                netProfit >= 0 ? ColorHint.SUCCESS : ColorHint.DANGER)))
                .build();

        GroupedSummary incomeSplit = GroupedSummary.builder()
                .key("incomeByCategory").label("Income (by Category)")
                .total(ReportingFormatUtil.round2(income))
                .formattedTotal(ReportingFormatUtil.compact(income))
                .items(List.of(
                        ReportingFactory.currencyValue("operatingIncome", "Operating Income", operatingIncome, ColorHint.SUCCESS),
                        ReportingFactory.currencyValue("otherIncome",     "Other Income",     otherIncome,     ColorHint.INFO)))
                .build();

        GroupedSummary expenseSplit = GroupedSummary.builder()
                .key("expenseByCategory").label("Expenses (by Category)")
                .total(ReportingFormatUtil.round2(expense))
                .formattedTotal(ReportingFormatUtil.compact(expense))
                .items(List.of(
                        ReportingFactory.currencyValue("directExpense",   "Direct Expense",   directExpense,   ColorHint.DANGER),
                        ReportingFactory.currencyValue("indirectExpense", "Indirect Expense", indirectExpense, ColorHint.WARNING)))
                .build();

        // --- Charts -------------------------------------------------------------
        List<ChartData> charts = List.of(
                financialReportService.revenueVsCollectionChart(organizationId, range));

        // --- Alerts -------------------------------------------------------------
        List<ExceptionItem> alerts = new ArrayList<>();
        if (netProfit < 0) {
            alerts.add(ExceptionItem.builder()
                    .key("negativeNetProfit").severity(Severity.WARNING)
                    .title("Negative net profit for the period")
                    .description("Expenses exceeded income in the selected period.")
                    .value(ReportingFormatUtil.round2(netProfit))
                    .formattedValue(ReportingFormatUtil.compact(netProfit))
                    .colorHint(ColorHint.DANGER)
                    .build());
        }
        if (liabilities > 0 && assets > 0 && liabilities > assets) {
            alerts.add(ExceptionItem.builder()
                    .key("payableExceedsAssets").severity(Severity.CRITICAL)
                    .title("Total payables exceed asset balance")
                    .description("Total liabilities exceed the total asset balance.")
                    .value(ReportingFormatUtil.round2(liabilities))
                    .formattedValue(ReportingFormatUtil.compact(liabilities))
                    .colorHint(ColorHint.DANGER)
                    .build());
        }

        return ReportingView.builder()
                .key("accountant-dashboard")
                .title("Accountant Dashboard")
                .subtitle("Finance overview derived from posted accounting entries")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(financialSummary, incomeSplit, expenseSplit))
                .charts(charts)
                .tables(List.of())
                .alerts(alerts)
                .build();
    }
}
