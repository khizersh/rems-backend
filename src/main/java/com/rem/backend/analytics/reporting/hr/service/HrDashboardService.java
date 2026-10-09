package com.rem.backend.analytics.reporting.hr.service;

import com.rem.backend.analytics.reporting.hr.query.HrAttendanceReportRepository;
import com.rem.backend.analytics.reporting.hr.query.HrLeaveReportRepository;
import com.rem.backend.analytics.reporting.hr.query.HrWorkforceReportRepository;
import com.rem.backend.analytics.reporting.hr.query.projection.LabeledCountProjection;
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
import com.rem.backend.payrollmanagement.entity.SalarySlip;
import com.rem.backend.payrollmanagement.repository.DepartmentRepository;
import com.rem.backend.payrollmanagement.repository.EmployeeRepository;
import com.rem.backend.payrollmanagement.repository.SalarySlipRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Composes the HR executive dashboard: workforce headcount, today's attendance snapshot,
 * leave approval backlog and current-month payroll cost, plus a hiring/attrition and
 * payroll-cost trend chart.
 * <p>
 * This service is a <em>composition</em> layer only; it delegates heavier chart building to
 * {@link HrWorkforceReportService} and {@link HrPayrollReportService} and pulls light
 * snapshot figures directly from the reporting repositories.
 */
@Service
@AllArgsConstructor
public class HrDashboardService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final long PENDING_LEAVE_ALERT_THRESHOLD = 5;
    private static final double LOW_ATTENDANCE_THRESHOLD = 80d;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final SalarySlipRepository salarySlipRepository;
    private final HrWorkforceReportRepository workforceReportRepo;
    private final HrAttendanceReportRepository attendanceReportRepo;
    private final HrLeaveReportRepository leaveReportRepo;
    private final HrWorkforceReportService workforceReportService;
    private final HrPayrollReportService payrollReportService;

    public ReportingView buildDashboard(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        LocalDate today = LocalDate.now();

        long totalEmployees = employeeRepository.countByOrganizationId(organizationId);
        long activeEmployees = employeeRepository.countByOrganizationIdAndStatus(organizationId, STATUS_ACTIVE);
        long departments = departmentRepository.countByOrganizationId(organizationId);

        List<LabeledCountProjection> todayAttendance = attendanceReportRepo.statusBreakdownForDate(organizationId, today);
        long presentToday = countFor(todayAttendance, "PRESENT");
        long absentToday = countFor(todayAttendance, "ABSENT");
        long onLeaveToday = countFor(todayAttendance, "ON_LEAVE");
        long attendanceLoggedToday = todayAttendance.stream().mapToLong(a -> a.getCount() == null ? 0 : a.getCount()).sum();
        double attendanceRateToday = attendanceLoggedToday == 0 ? 0d : (presentToday * 100d) / attendanceLoggedToday;

        long pendingLeave = leaveReportRepo.countPending(organizationId);

        int year = today.getYear();
        int month = today.getMonthValue();
        List<SalarySlip> currentMonthSlips = salarySlipRepository.findByOrganizationIdAndSalaryMonthAndSalaryYear(organizationId, month, year);
        double currentMonthPayroll = currentMonthSlips.stream()
                .map(SalarySlip::getNetSalary).filter(java.util.Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).sum();

        List<KpiCard> cards = new ArrayList<>();
        cards.add(ReportingFactory.countCard("totalEmployees", "Total Employees", totalEmployees, "FaUsers", ColorHint.PRIMARY));
        cards.add(ReportingFactory.countCard("activeEmployees", "Active Employees", activeEmployees, "FaUserCheck", ColorHint.SUCCESS));
        cards.add(ReportingFactory.countCard("totalDepartments", "Departments", departments, "FaSitemap", ColorHint.INFO));
        cards.add(ReportingFactory.countCard("presentToday", "Present Today", presentToday, "FaUserClock", ColorHint.SUCCESS));
        cards.add(ReportingFactory.countCard("absentToday", "Absent Today", absentToday, "FaUserXmark", ColorHint.DANGER));
        cards.add(ReportingFactory.countCard("onLeaveToday", "On Leave Today", onLeaveToday, "FaUmbrellaBeach", ColorHint.WARNING));
        cards.add(ReportingFactory.countCard("pendingLeaveRequests", "Pending Leave Requests", pendingLeave, "FaHourglassHalf", ColorHint.WARNING));
        cards.add(ReportingFactory.currencyCard("payrollCostThisMonth", "Payroll Cost (This Month)", currentMonthPayroll, "FaMoneyCheckDollar", ColorHint.WARNING));

        GroupedSummary employeeStatus = GroupedSummary.builder()
                .key("employeeStatus").label("Employee Status")
                .total(totalEmployees)
                .items(workforceReportRepo.headcountByStatus(organizationId).stream()
                        .map(s -> ReportingFactory.countValue(slug(s.getLabel()), s.getLabel(),
                                s.getCount() == null ? 0 : s.getCount(), ColorHint.NEUTRAL))
                        .toList())
                .build();

        GroupedSummary attendanceToday = GroupedSummary.builder()
                .key("attendanceToday").label("Attendance Today")
                .total(attendanceLoggedToday)
                .items(todayAttendance.stream()
                        .map(a -> ReportingFactory.countValue(slug(a.getLabel()), a.getLabel(),
                                a.getCount() == null ? 0 : a.getCount(), ColorHint.NEUTRAL))
                        .toList())
                .build();

        // Reuse the richer trend charts already built for the drill-down reports.
        ReportingView employeeOverview = workforceReportService.employeeOverview(organizationId, filter);
        ReportingView payrollOverview = payrollReportService.payrollOverview(organizationId, filter);
        List<ChartData> charts = new ArrayList<>();
        if (!employeeOverview.getCharts().isEmpty()) {
            charts.add(employeeOverview.getCharts().get(0));
        }
        if (!payrollOverview.getCharts().isEmpty()) {
            charts.add(payrollOverview.getCharts().get(0));
        }

        List<ExceptionItem> alerts = new ArrayList<>();
        if (pendingLeave >= PENDING_LEAVE_ALERT_THRESHOLD) {
            alerts.add(ExceptionItem.builder()
                    .key("pendingLeaveBacklog").severity(Severity.WARNING)
                    .title("Leave approval backlog")
                    .description(pendingLeave + " leave requests are awaiting approval.")
                    .value((double) pendingLeave)
                    .formattedValue(ReportingFormatUtil.number(pendingLeave))
                    .colorHint(ColorHint.WARNING)
                    .build());
        }
        if (attendanceLoggedToday > 0 && attendanceRateToday < LOW_ATTENDANCE_THRESHOLD) {
            alerts.add(ExceptionItem.builder()
                    .key("lowAttendanceToday").severity(Severity.WARNING)
                    .title("Attendance rate below 80% today")
                    .description("Review absenteeism drivers across departments.")
                    .value(ReportingFormatUtil.round2(attendanceRateToday))
                    .formattedValue(ReportingFormatUtil.compact(attendanceRateToday) + "%")
                    .colorHint(ColorHint.WARNING)
                    .build());
        }

        return ReportingView.builder()
                .key("hr-dashboard")
                .title("HR Dashboard")
                .subtitle("Workforce, attendance, leave and payroll overview")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(employeeStatus, attendanceToday))
                .charts(charts)
                .tables(List.of())
                .alerts(alerts)
                .build();
    }

    private static long countFor(List<LabeledCountProjection> rows, String label) {
        for (LabeledCountProjection row : rows) {
            if (label.equalsIgnoreCase(row.getLabel())) {
                return row.getCount() == null ? 0 : row.getCount();
            }
        }
        return 0L;
    }

    private static String slug(String label) {
        return label == null ? "unknown" : label.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }
}
