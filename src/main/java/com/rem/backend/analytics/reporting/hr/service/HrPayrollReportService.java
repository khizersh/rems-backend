package com.rem.backend.analytics.reporting.hr.service;

import com.rem.backend.analytics.reporting.hr.query.HrPayrollReportRepository;
import com.rem.backend.analytics.reporting.hr.query.projection.LabeledAmountProjection;
import com.rem.backend.analytics.reporting.hr.query.projection.LabeledCountProjection;
import com.rem.backend.analytics.reporting.shared.dto.ChartData;
import com.rem.backend.analytics.reporting.shared.dto.ChartSeries;
import com.rem.backend.analytics.reporting.shared.dto.GroupedSummary;
import com.rem.backend.analytics.reporting.shared.dto.KpiCard;
import com.rem.backend.analytics.reporting.shared.dto.LabeledValue;
import com.rem.backend.analytics.reporting.shared.dto.ReportingView;
import com.rem.backend.analytics.reporting.shared.dto.TableColumn;
import com.rem.backend.analytics.reporting.shared.dto.TableData;
import com.rem.backend.analytics.reporting.shared.enums.ChartType;
import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.enums.ValueType;
import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;
import com.rem.backend.analytics.reporting.shared.util.DateRange;
import com.rem.backend.analytics.reporting.shared.util.ReportingDateUtil;
import com.rem.backend.analytics.reporting.shared.util.ReportingFactory;
import com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil;
import com.rem.backend.payrollmanagement.entity.Payroll;
import com.rem.backend.payrollmanagement.entity.SalaryAmendment;
import com.rem.backend.payrollmanagement.entity.SalarySlip;
import com.rem.backend.payrollmanagement.repository.SalaryAmendmentRepository;
import com.rem.backend.payrollmanagement.repository.SalarySlipRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil.nz;

/**
 * HR reporting for payroll cost and compensation structure. Payroll totals for a specific
 * month/year come from {@link SalarySlip} (per-employee, denormalized) and {@link Payroll}
 * (per-run summary); allowance/deduction composition reflects the current recurring setup
 * on employee records.
 */
@Service
@AllArgsConstructor
public class HrPayrollReportService {

    private static final int TOP_EARNERS_LIMIT = 10;

    private final SalarySlipRepository salarySlipRepository;
    private final SalaryAmendmentRepository salaryAmendmentRepository;
    private final HrPayrollReportRepository payrollReportRepo;

    public ReportingView payrollOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        int year = filter != null && filter.getYear() != null ? filter.getYear() : LocalDate.now().getYear();
        int month = filter != null && filter.getMonth() != null ? filter.getMonth() : range.getEnd().getMonthValue();

        List<SalarySlip> slips = salarySlipRepository.findByOrganizationIdAndSalaryMonthAndSalaryYear(organizationId, month, year);
        double netSalaryTotal = sum(slips, SalarySlip::getNetSalary);
        double grossSalaryTotal = sum(slips, SalarySlip::getGrossSalary);
        double allowancesTotal = sum(slips, SalarySlip::getTotalAllowances);
        double deductionsTotal = sum(slips, SalarySlip::getTotalDeductions);
        long slipsGenerated = slips.size();
        double avgNetSalary = slipsGenerated == 0 ? 0d : netSalaryTotal / slipsGenerated;

        List<SalaryAmendment> amendments = salaryAmendmentRepository.findByOrganizationIdAndSalaryMonthAndSalaryYear(organizationId, month, year);
        double additions = amendments.stream()
                .filter(a -> "ADDITION".equalsIgnoreCase(a.getAmendmentType()))
                .map(SalaryAmendment::getAmount).filter(java.util.Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).sum();
        double deductionAmendments = amendments.stream()
                .filter(a -> "DEDUCTION".equalsIgnoreCase(a.getAmendmentType()))
                .map(SalaryAmendment::getAmount).filter(java.util.Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).sum();

        List<KpiCard> cards = List.of(
                ReportingFactory.currencyCard("netPayrollCost", "Net Payroll Cost (Selected Month)", netSalaryTotal, "FaMoneyCheckDollar", ColorHint.WARNING),
                ReportingFactory.currencyCard("grossPayrollCost", "Gross Payroll Cost (Selected Month)", grossSalaryTotal, "FaSackDollar", ColorHint.INFO),
                ReportingFactory.currencyCard("avgNetSalary", "Average Net Salary", avgNetSalary, "FaCoins", ColorHint.PRIMARY),
                ReportingFactory.countCard("slipsGenerated", "Salary Slips Generated", slipsGenerated, "FaFileInvoice", ColorHint.NEUTRAL),
                ReportingFactory.currencyCard("amendmentAdditions", "Amendment Additions", additions, "FaArrowTrendUp", ColorHint.SUCCESS),
                ReportingFactory.currencyCard("amendmentDeductions", "Amendment Deductions", deductionAmendments, "FaArrowTrendDown", ColorHint.DANGER)
        );

        GroupedSummary payrollSummary = GroupedSummary.builder()
                .key("payrollSummary").label("Payroll Summary (Selected Month)")
                .total(ReportingFormatUtil.round2(netSalaryTotal))
                .formattedTotal(ReportingFormatUtil.compact(netSalaryTotal))
                .items(List.of(
                        ReportingFactory.currencyValue("grossSalary", "Gross Salary", grossSalaryTotal, ColorHint.INFO),
                        ReportingFactory.currencyValue("allowances", "Allowances", allowancesTotal, ColorHint.SUCCESS),
                        ReportingFactory.currencyValue("deductions", "Deductions", deductionsTotal, ColorHint.DANGER),
                        ReportingFactory.currencyValue("netSalary", "Net Salary", netSalaryTotal, ColorHint.PRIMARY)))
                .build();

        GroupedSummary slipStatus = toBreakdown("salarySlipStatus", "Salary Slip Status",
                payrollReportRepo.salarySlipStatusBreakdown(organizationId, month, year));

        List<LabeledAmountProjection> byDept = payrollReportRepo.netSalaryByDepartment(organizationId, month, year);
        GroupedSummary deptCost = GroupedSummary.builder()
                .key("payrollByDepartment").label("Payroll Cost by Department")
                .total(ReportingFormatUtil.round2(netSalaryTotal))
                .formattedTotal(ReportingFormatUtil.compact(netSalaryTotal))
                .items(byDept.stream()
                        .map(d -> ReportingFactory.currencyValue(slug(d.getLabel()), d.getLabel(), nz(d.getAmount()), ColorHint.INFO))
                        .toList())
                .build();

        ChartData payrollTrend = monthlyPayrollTrendChart(organizationId, range);

        List<Map<String, Object>> topEarnerRows = payrollReportRepo.topEarners(organizationId, month, year, TOP_EARNERS_LIMIT);
        List<TableColumn> columns = List.of(
                TableColumn.builder().key("employeeName").label("Employee").type(ValueType.TEXT).build(),
                TableColumn.builder().key("departmentName").label("Department").type(ValueType.TEXT).build(),
                TableColumn.builder().key("designation").label("Designation").type(ValueType.TEXT).build(),
                TableColumn.builder().key("netSalary").label("Net Salary").type(ValueType.CURRENCY).build()
        );
        TableData topEarners = TableData.builder()
                .key("topEarners").title("Top Earners (Selected Month)")
                .columns(columns).rows(topEarnerRows).build();

        return ReportingView.builder()
                .key("payroll-overview").title("Payroll Overview")
                .subtitle("Payroll cost, composition and department distribution")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(payrollSummary, slipStatus, deptCost))
                .charts(List.of(payrollTrend))
                .tables(List.of(topEarners))
                .alerts(List.of())
                .build();
    }

    public ReportingView compensationBreakdown(long organizationId, ReportingFilter filter) {
        List<LabeledAmountProjection> allowances = payrollReportRepo.allowanceTotalsByName(organizationId);
        List<LabeledAmountProjection> deductions = payrollReportRepo.deductionTotalsByName(organizationId);

        double totalAllowances = allowances.stream().mapToDouble(a -> nz(a.getAmount())).sum();
        double totalDeductions = deductions.stream().mapToDouble(d -> nz(d.getAmount())).sum();

        List<KpiCard> cards = List.of(
                ReportingFactory.currencyCard("totalRecurringAllowances", "Total Recurring Allowances", totalAllowances, "FaHandHoldingDollar", ColorHint.SUCCESS),
                ReportingFactory.currencyCard("totalRecurringDeductions", "Total Recurring Deductions", totalDeductions, "FaFileInvoiceDollar", ColorHint.DANGER)
        );

        GroupedSummary allowanceBreakdown = GroupedSummary.builder()
                .key("allowanceBreakdown").label("Allowances by Type")
                .total(ReportingFormatUtil.round2(totalAllowances))
                .formattedTotal(ReportingFormatUtil.compact(totalAllowances))
                .items(allowances.stream()
                        .map(a -> ReportingFactory.currencyValue(slug(a.getLabel()), a.getLabel(), nz(a.getAmount()), ColorHint.SUCCESS))
                        .toList())
                .build();

        GroupedSummary deductionBreakdown = GroupedSummary.builder()
                .key("deductionBreakdown").label("Deductions by Type")
                .total(ReportingFormatUtil.round2(totalDeductions))
                .formattedTotal(ReportingFormatUtil.compact(totalDeductions))
                .items(deductions.stream()
                        .map(d -> ReportingFactory.currencyValue(slug(d.getLabel()), d.getLabel(), nz(d.getAmount()), ColorHint.DANGER))
                        .toList())
                .build();

        ChartData allowanceChart = pie("allowanceDistribution", "Allowance Distribution", allowances);
        ChartData deductionChart = pie("deductionDistribution", "Deduction Distribution", deductions);

        return ReportingView.builder()
                .key("compensation-breakdown").title("Compensation Breakdown")
                .subtitle("Current recurring allowance and deduction structure")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(allowanceBreakdown, deductionBreakdown))
                .charts(List.of(allowanceChart, deductionChart))
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    // ---------------------------------------------------------------------

    /** Builds the monthly net-payroll trend from processed {@link Payroll} runs within range. */
    private ChartData monthlyPayrollTrendChart(long organizationId, DateRange range) {
        List<YearMonth> buckets = ReportingDateUtil.monthBuckets(range);
        Map<YearMonth, Double> byMonth = new LinkedHashMap<>();
        for (Payroll p : payrollReportRepo.findAllByOrganizationIdOrdered(organizationId)) {
            if (p.getPayrollYear() != null && p.getPayrollMonth() != null && p.getTotalNetSalary() != null) {
                byMonth.put(YearMonth.of(p.getPayrollYear(), p.getPayrollMonth()), p.getTotalNetSalary().doubleValue());
            }
        }
        List<String> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (YearMonth ym : buckets) {
            labels.add(ReportingDateUtil.monthLabel(ym));
            values.add(byMonth.getOrDefault(ym, 0d));
        }
        return ChartData.builder()
                .key("monthlyPayrollTrend").title("Monthly Payroll Cost").type(ChartType.LINE)
                .labels(labels)
                .series(List.of(ChartSeries.builder().name("Net Payroll").data(values).colorHint(ColorHint.WARNING).build()))
                .build();
    }

    private ChartData pie(String key, String title, List<LabeledAmountProjection> rows) {
        List<String> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (LabeledAmountProjection row : rows) {
            labels.add(row.getLabel());
            values.add(nz(row.getAmount()));
        }
        return ChartData.builder()
                .key(key).title(title).type(ChartType.DONUT)
                .labels(labels)
                .series(List.of(ChartSeries.builder().name("Amount").data(values).build()))
                .build();
    }

    private GroupedSummary toBreakdown(String key, String label, List<LabeledCountProjection> rows) {
        long total = 0;
        List<LabeledValue> items = new ArrayList<>();
        for (LabeledCountProjection row : rows) {
            long count = row.getCount() == null ? 0 : row.getCount();
            total += count;
            items.add(ReportingFactory.countValue(slug(row.getLabel()), row.getLabel(), count, ColorHint.NEUTRAL));
        }
        return GroupedSummary.builder().key(key).label(label).total(total).items(items).build();
    }

    private static double sum(List<SalarySlip> slips, java.util.function.Function<SalarySlip, BigDecimal> extractor) {
        BigDecimal total = BigDecimal.ZERO;
        for (SalarySlip slip : slips) {
            BigDecimal value = extractor.apply(slip);
            if (value != null) {
                total = total.add(value);
            }
        }
        return total.doubleValue();
    }

    private static String slug(String label) {
        return label == null ? "unknown" : label.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }
}
