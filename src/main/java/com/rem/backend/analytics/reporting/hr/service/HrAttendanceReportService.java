package com.rem.backend.analytics.reporting.hr.service;

import com.rem.backend.analytics.reporting.hr.query.HrAttendanceReportRepository;
import com.rem.backend.analytics.reporting.hr.query.HrLeaveReportRepository;
import com.rem.backend.analytics.reporting.hr.query.projection.LabeledAmountProjection;
import com.rem.backend.analytics.reporting.hr.query.projection.LabeledCountProjection;
import com.rem.backend.analytics.reporting.hr.query.projection.MonthlyAmountSumProjection;
import com.rem.backend.analytics.reporting.hr.query.projection.MonthlyAttendanceProjection;
import com.rem.backend.analytics.reporting.shared.dto.ChartData;
import com.rem.backend.analytics.reporting.shared.dto.ChartSeries;
import com.rem.backend.analytics.reporting.shared.dto.ExceptionItem;
import com.rem.backend.analytics.reporting.shared.dto.GroupedSummary;
import com.rem.backend.analytics.reporting.shared.dto.KpiCard;
import com.rem.backend.analytics.reporting.shared.dto.LabeledValue;
import com.rem.backend.analytics.reporting.shared.dto.ReportingView;
import com.rem.backend.analytics.reporting.shared.dto.TableColumn;
import com.rem.backend.analytics.reporting.shared.dto.TableData;
import com.rem.backend.analytics.reporting.shared.enums.ChartType;
import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.enums.Severity;
import com.rem.backend.analytics.reporting.shared.enums.ValueType;
import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;
import com.rem.backend.analytics.reporting.shared.util.DateRange;
import com.rem.backend.analytics.reporting.shared.util.ReportingDateUtil;
import com.rem.backend.analytics.reporting.shared.util.ReportingFactory;
import com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil.nz;

/**
 * HR reporting for daily attendance and leave management: status breakdowns, attendance-rate
 * trend, leave utilisation by type and a pending-approvals drill-down table.
 */
@Service
@AllArgsConstructor
public class HrAttendanceReportService {

    /** Pending leave requests above this count are surfaced as a dashboard alert. */
    private static final long PENDING_LEAVE_ALERT_THRESHOLD = 5;
    private static final int PENDING_LEAVE_TABLE_LIMIT = 20;

    private final HrAttendanceReportRepository attendanceReportRepo;
    private final HrLeaveReportRepository leaveReportRepo;

    public ReportingView attendanceOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        LocalDate today = LocalDate.now();

        List<LabeledCountProjection> periodBreakdown = attendanceReportRepo.statusBreakdown(
                organizationId, range.getStart().toLocalDate(), range.getEnd().toLocalDate());
        List<LabeledCountProjection> todayBreakdown = attendanceReportRepo.statusBreakdownForDate(organizationId, today);

        long presentToday = countFor(todayBreakdown, "PRESENT");
        long absentToday = countFor(todayBreakdown, "ABSENT");
        long lateToday = countFor(todayBreakdown, "LATE");
        long onLeaveToday = countFor(todayBreakdown, "ON_LEAVE");

        long presentPeriod = countFor(periodBreakdown, "PRESENT");
        long totalPeriod = periodBreakdown.stream().mapToLong(p -> p.getCount() == null ? 0 : p.getCount()).sum();
        double attendanceRatePeriod = totalPeriod == 0 ? 0d : (presentPeriod * 100d) / totalPeriod;

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("presentToday", "Present Today", presentToday, "FaUserCheck", ColorHint.SUCCESS),
                ReportingFactory.countCard("absentToday", "Absent Today", absentToday, "FaUserXmark", ColorHint.DANGER),
                ReportingFactory.countCard("lateToday", "Late Today", lateToday, "FaClock", ColorHint.WARNING),
                ReportingFactory.countCard("onLeaveToday", "On Leave Today", onLeaveToday, "FaUmbrellaBeach", ColorHint.INFO),
                ReportingFactory.currencyCard("attendanceRatePeriod", "Attendance Rate (Period) %", attendanceRatePeriod, "FaChartLine",
                        attendanceRatePeriod >= 90 ? ColorHint.SUCCESS : ColorHint.WARNING)
        );

        GroupedSummary statusBreakdown = toBreakdown("attendanceStatusPeriod", "Attendance Status (Period)", periodBreakdown);

        List<MonthlyAttendanceProjection> monthly = attendanceReportRepo.monthlyAttendanceRate(
                organizationId, range.getStart().toLocalDate(), range.getEnd().toLocalDate());
        ChartData rateTrend = attendanceRateChart(range, monthly);

        List<ExceptionItem> alerts = new ArrayList<>();
        if (attendanceRatePeriod > 0 && attendanceRatePeriod < 80) {
            alerts.add(ExceptionItem.builder()
                    .key("lowAttendanceRate").severity(Severity.WARNING)
                    .title("Attendance rate below 80% for the selected period")
                    .description("Review absenteeism drivers across departments.")
                    .value(ReportingFormatUtil.round2(attendanceRatePeriod))
                    .formattedValue(ReportingFormatUtil.compact(attendanceRatePeriod) + "%")
                    .colorHint(ColorHint.WARNING)
                    .build());
        }

        return ReportingView.builder()
                .key("attendance-overview").title("Attendance Overview")
                .subtitle("Daily attendance snapshot and period trend")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(statusBreakdown))
                .charts(List.of(rateTrend))
                .tables(List.of())
                .alerts(alerts)
                .build();
    }

    public ReportingView leaveOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);

        List<LabeledCountProjection> statusBreakdown = leaveReportRepo.statusBreakdown(
                organizationId, range.getStart().toLocalDate(), range.getEnd().toLocalDate());
        long pending = countFor(statusBreakdown, "PENDING");
        long approved = countFor(statusBreakdown, "APPROVED");
        long rejected = countFor(statusBreakdown, "REJECTED");
        long cancelled = countFor(statusBreakdown, "CANCELLED");

        List<LabeledAmountProjection> daysByType = leaveReportRepo.leaveDaysByType(
                organizationId, range.getStart().toLocalDate(), range.getEnd().toLocalDate());
        double totalApprovedDays = daysByType.stream().mapToDouble(d -> nz(d.getAmount())).sum();

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("pendingLeaveRequests", "Pending Requests", pending, "FaHourglassHalf", ColorHint.WARNING),
                ReportingFactory.countCard("approvedLeaveRequests", "Approved Requests", approved, "FaCircleCheck", ColorHint.SUCCESS),
                ReportingFactory.countCard("rejectedLeaveRequests", "Rejected Requests", rejected, "FaCircleXmark", ColorHint.DANGER),
                ReportingFactory.countCard("cancelledLeaveRequests", "Cancelled Requests", cancelled, "FaBan", ColorHint.NEUTRAL),
                ReportingFactory.countCard("approvedLeaveDays", "Approved Leave Days (Period)", totalApprovedDays, "FaCalendarDays", ColorHint.INFO)
        );

        GroupedSummary requestStatus = toBreakdown("leaveRequestStatus", "Leave Request Status", statusBreakdown);
        GroupedSummary daysByTypeBreakdown = GroupedSummary.builder()
                .key("leaveDaysByType").label("Approved Leave Days by Type")
                .total(ReportingFormatUtil.round2(totalApprovedDays))
                .formattedTotal(ReportingFormatUtil.compact(totalApprovedDays))
                .items(daysByType.stream()
                        .map(d -> ReportingFactory.countValue(slug(d.getLabel()), d.getLabel(), nz(d.getAmount()), ColorHint.PRIMARY))
                        .toList())
                .build();

        List<MonthlyAmountSumProjection> monthlyDays = leaveReportRepo.monthlyApprovedLeaveDays(
                organizationId, range.getStart().toLocalDate(), range.getEnd().toLocalDate());
        ChartData trend = monthlyAmountChart("leaveDaysTrend", "Approved Leave Days (Monthly)", "Leave Days", range, monthlyDays);

        List<Map<String, Object>> pendingRows = leaveReportRepo.pendingLeaveRequests(organizationId, PENDING_LEAVE_TABLE_LIMIT);
        List<TableColumn> columns = List.of(
                TableColumn.builder().key("employeeName").label("Employee").type(ValueType.TEXT).build(),
                TableColumn.builder().key("departmentName").label("Department").type(ValueType.TEXT).build(),
                TableColumn.builder().key("leaveType").label("Leave Type").type(ValueType.TEXT).build(),
                TableColumn.builder().key("startDate").label("Start Date").type(ValueType.DATE).build(),
                TableColumn.builder().key("endDate").label("End Date").type(ValueType.DATE).build(),
                TableColumn.builder().key("totalDays").label("Days").type(ValueType.NUMBER).build(),
                TableColumn.builder().key("reason").label("Reason").type(ValueType.TEXT).build()
        );
        TableData pendingTable = TableData.builder()
                .key("pendingLeaveRequests").title("Pending Leave Requests")
                .columns(columns).rows(pendingRows).build();

        List<ExceptionItem> alerts = new ArrayList<>();
        long totalPending = leaveReportRepo.countPending(organizationId);
        if (totalPending >= PENDING_LEAVE_ALERT_THRESHOLD) {
            alerts.add(ExceptionItem.builder()
                    .key("pendingLeaveBacklog").severity(Severity.WARNING)
                    .title("Leave approval backlog")
                    .description(totalPending + " leave requests are awaiting approval.")
                    .value((double) totalPending)
                    .formattedValue(ReportingFormatUtil.number(totalPending))
                    .colorHint(ColorHint.WARNING)
                    .build());
        }

        return ReportingView.builder()
                .key("leave-overview").title("Leave Overview")
                .subtitle("Leave requests, approvals and utilisation")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(requestStatus, daysByTypeBreakdown))
                .charts(List.of(trend))
                .tables(List.of(pendingTable))
                .alerts(alerts)
                .build();
    }

    // ---------------------------------------------------------------------

    private ChartData attendanceRateChart(DateRange range, List<MonthlyAttendanceProjection> monthly) {
        List<YearMonth> buckets = ReportingDateUtil.monthBuckets(range);
        Map<YearMonth, Double> rateByMonth = new LinkedHashMap<>();
        for (MonthlyAttendanceProjection m : monthly) {
            if (m.getYr() != null && m.getMonthNo() != null) {
                long present = m.getPresentCount() == null ? 0 : m.getPresentCount();
                long total = m.getTotalCount() == null ? 0 : m.getTotalCount();
                double rate = total == 0 ? 0d : (present * 100d) / total;
                rateByMonth.put(YearMonth.of(m.getYr(), m.getMonthNo()), ReportingFormatUtil.round2(rate));
            }
        }
        List<String> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (YearMonth ym : buckets) {
            labels.add(ReportingDateUtil.monthLabel(ym));
            values.add(rateByMonth.getOrDefault(ym, 0d));
        }
        return ChartData.builder()
                .key("attendanceRateTrend").title("Attendance Rate Trend").type(ChartType.LINE)
                .labels(labels)
                .series(List.of(ChartSeries.builder().name("Attendance Rate %").data(values).colorHint(ColorHint.PRIMARY).build()))
                .build();
    }

    private ChartData monthlyAmountChart(String key, String title, String seriesName, DateRange range,
                                         List<MonthlyAmountSumProjection> data) {
        List<YearMonth> buckets = ReportingDateUtil.monthBuckets(range);
        Map<YearMonth, Double> byMonth = new LinkedHashMap<>();
        for (MonthlyAmountSumProjection d : data) {
            if (d.getYr() != null && d.getMonthNo() != null) {
                byMonth.put(YearMonth.of(d.getYr(), d.getMonthNo()), nz(d.getAmount()));
            }
        }
        List<String> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (YearMonth ym : buckets) {
            labels.add(ReportingDateUtil.monthLabel(ym));
            values.add(byMonth.getOrDefault(ym, 0d));
        }
        return ChartData.builder()
                .key(key).title(title).type(ChartType.BAR)
                .labels(labels)
                .series(List.of(ChartSeries.builder().name(seriesName).data(values).colorHint(ColorHint.INFO).build()))
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
