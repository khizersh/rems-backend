package com.rem.backend.analytics.reporting.hr.service;

import com.rem.backend.analytics.reporting.hr.query.HrPayrollReportRepository;
import com.rem.backend.analytics.reporting.hr.query.HrWorkforceReportRepository;
import com.rem.backend.analytics.reporting.hr.query.projection.LabeledAmountProjection;
import com.rem.backend.analytics.reporting.hr.query.projection.LabeledCountProjection;
import com.rem.backend.analytics.reporting.hr.query.projection.MonthlyCountProjection;
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
import com.rem.backend.payrollmanagement.entity.Department;
import com.rem.backend.payrollmanagement.repository.DepartmentRepository;
import com.rem.backend.payrollmanagement.repository.EmployeeRepository;
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
 * HR reporting for workforce composition: headcount, employment mix, hiring/attrition trend
 * and department structure (headcount + budget vs. actual payroll cost).
 */
@Service
@AllArgsConstructor
public class HrWorkforceReportService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final HrWorkforceReportRepository workforceReportRepo;
    private final HrPayrollReportRepository payrollReportRepo;

    public ReportingView employeeOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);

        long totalEmployees = employeeRepository.countByOrganizationId(organizationId);
        long activeEmployees = employeeRepository.countByOrganizationIdAndStatus(organizationId, STATUS_ACTIVE);
        double avgBasicSalary = nz(workforceReportRepo.averageActiveBasicSalary(organizationId));

        List<MonthlyCountProjection> hires = workforceReportRepo.monthlyHires(
                organizationId, range.getStart().toLocalDate(), range.getEnd().toLocalDate());
        List<MonthlyCountProjection> separations = workforceReportRepo.monthlyTerminations(
                organizationId, range.getStart().toLocalDate(), range.getEnd().toLocalDate());
        long newHiresInPeriod = hires.stream().mapToLong(h -> h.getCount() == null ? 0 : h.getCount()).sum();
        long separationsInPeriod = separations.stream().mapToLong(s -> s.getCount() == null ? 0 : s.getCount()).sum();

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("totalEmployees", "Total Employees", totalEmployees, "FaUsers", ColorHint.PRIMARY),
                ReportingFactory.countCard("activeEmployees", "Active Employees", activeEmployees, "FaUserCheck", ColorHint.SUCCESS),
                ReportingFactory.countCard("newHires", "New Hires (Period)", newHiresInPeriod, "FaUserPlus", ColorHint.SUCCESS),
                ReportingFactory.countCard("separations", "Separations (Period)", separationsInPeriod, "FaUserMinus", ColorHint.DANGER),
                ReportingFactory.currencyCard("avgBasicSalary", "Average Basic Salary", avgBasicSalary, "FaSackDollar", ColorHint.INFO)
        );

        GroupedSummary statusBreakdown = toBreakdown("employeeStatus", "Employee Status",
                workforceReportRepo.headcountByStatus(organizationId));
        GroupedSummary typeBreakdown = toBreakdown("employmentType", "Employment Type",
                workforceReportRepo.headcountByEmploymentType(organizationId));
        GroupedSummary genderBreakdown = toBreakdown("genderDistribution", "Gender Distribution",
                workforceReportRepo.headcountByGender(organizationId));

        ChartData hiringTrend = hiringVsAttritionChart(range, hires, separations);

        return ReportingView.builder()
                .key("employee-overview").title("Employee Overview")
                .subtitle("Headcount, composition and hiring trend")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(statusBreakdown, typeBreakdown, genderBreakdown))
                .charts(List.of(hiringTrend))
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    public ReportingView departmentOverview(long organizationId, ReportingFilter filter) {
        List<Department> departments = departmentRepository.findByOrganizationId(organizationId);
        long activeDepartments = departments.stream().filter(d -> Boolean.TRUE.equals(d.getIsActive())).count();

        BigDecimal totalBudget = BigDecimal.ZERO;
        for (Department d : departments) {
            if (d.getBudgetAllocated() != null) {
                totalBudget = totalBudget.add(d.getBudgetAllocated());
            }
        }

        int year = filter != null && filter.getYear() != null ? filter.getYear() : LocalDate.now().getYear();
        int month = filter != null && filter.getMonth() != null ? filter.getMonth() : LocalDate.now().getMonthValue();
        List<LabeledAmountProjection> payrollByDept = payrollReportRepo.netSalaryByDepartment(organizationId, month, year);
        double totalPayrollCost = payrollByDept.stream().mapToDouble(p -> nz(p.getAmount())).sum();

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("totalDepartments", "Total Departments", departments.size(), "FaSitemap", ColorHint.PRIMARY),
                ReportingFactory.countCard("activeDepartments", "Active Departments", activeDepartments, "FaCircleCheck", ColorHint.SUCCESS),
                ReportingFactory.currencyCard("budgetAllocated", "Total Budget Allocated", totalBudget.doubleValue(), "FaWallet", ColorHint.INFO),
                ReportingFactory.currencyCard("payrollCostSelectedMonth", "Payroll Cost (Selected Month)", totalPayrollCost, "FaMoneyCheckDollar", ColorHint.WARNING)
        );

        GroupedSummary headcountByDept = toBreakdown("headcountByDepartment", "Headcount by Department",
                workforceReportRepo.headcountByDepartment(organizationId));

        List<TableColumn> columns = List.of(
                TableColumn.builder().key("department").label("Department").type(ValueType.TEXT).build(),
                TableColumn.builder().key("headcount").label("Headcount").type(ValueType.NUMBER).build(),
                TableColumn.builder().key("budgetAllocated").label("Budget Allocated").type(ValueType.CURRENCY).build(),
                TableColumn.builder().key("payrollCost").label("Payroll Cost (Selected Month)").type(ValueType.CURRENCY).build()
        );
        Map<String, Long> headcountMap = new LinkedHashMap<>();
        for (LabeledCountProjection p : workforceReportRepo.headcountByDepartment(organizationId)) {
            headcountMap.put(p.getLabel(), p.getCount());
        }
        Map<String, Double> payrollMap = new LinkedHashMap<>();
        for (LabeledAmountProjection p : payrollByDept) {
            payrollMap.put(p.getLabel(), nz(p.getAmount()));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Department d : departments) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("department", d.getName());
            row.put("headcount", headcountMap.getOrDefault(d.getName(), 0L));
            row.put("budgetAllocated", ReportingFormatUtil.round2(
                    d.getBudgetAllocated() == null ? 0d : d.getBudgetAllocated().doubleValue()));
            row.put("payrollCost", ReportingFormatUtil.round2(payrollMap.getOrDefault(d.getName(), 0d)));
            rows.add(row);
        }
        TableData table = TableData.builder()
                .key("departmentTable").title("Department Breakdown")
                .columns(columns).rows(rows).build();

        return ReportingView.builder()
                .key("department-overview").title("Department Overview")
                .subtitle("Structure, headcount and payroll cost by department")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(headcountByDept))
                .charts(List.of())
                .tables(List.of(table))
                .alerts(List.of())
                .build();
    }

    // ---------------------------------------------------------------------

    private ChartData hiringVsAttritionChart(DateRange range, List<MonthlyCountProjection> hires,
                                             List<MonthlyCountProjection> separations) {
        List<YearMonth> buckets = ReportingDateUtil.monthBuckets(range);
        Map<YearMonth, Long> hiresByMonth = new LinkedHashMap<>();
        for (MonthlyCountProjection h : hires) {
            if (h.getYr() != null && h.getMonthNo() != null) {
                hiresByMonth.put(YearMonth.of(h.getYr(), h.getMonthNo()), h.getCount() == null ? 0L : h.getCount());
            }
        }
        Map<YearMonth, Long> separationsByMonth = new LinkedHashMap<>();
        for (MonthlyCountProjection s : separations) {
            if (s.getYr() != null && s.getMonthNo() != null) {
                separationsByMonth.put(YearMonth.of(s.getYr(), s.getMonthNo()), s.getCount() == null ? 0L : s.getCount());
            }
        }
        List<String> labels = new ArrayList<>();
        List<Object> hireValues = new ArrayList<>();
        List<Object> separationValues = new ArrayList<>();
        for (YearMonth ym : buckets) {
            labels.add(ReportingDateUtil.monthLabel(ym));
            hireValues.add(hiresByMonth.getOrDefault(ym, 0L));
            separationValues.add(separationsByMonth.getOrDefault(ym, 0L));
        }
        return ChartData.builder()
                .key("hiringVsAttrition").title("Hiring vs Attrition").type(ChartType.BAR)
                .labels(labels)
                .series(List.of(
                        ChartSeries.builder().name("New Hires").data(hireValues).colorHint(ColorHint.SUCCESS).build(),
                        ChartSeries.builder().name("Separations").data(separationValues).colorHint(ColorHint.DANGER).build()))
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

    private static String slug(String label) {
        return label == null ? "unknown" : label.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }
}
