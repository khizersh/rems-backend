package com.rem.backend.analytics.reporting.admin.service;

import com.rem.backend.accountingmanagement.utility.JournalUtilities;
import com.rem.backend.analytics.reporting.admin.query.AdminAccountingReportRepository;
import com.rem.backend.analytics.reporting.admin.query.AdminBookingReportRepository;
import com.rem.backend.analytics.reporting.shared.dto.ChartData;
import com.rem.backend.analytics.reporting.shared.dto.ChartSeries;
import com.rem.backend.analytics.reporting.shared.dto.ExceptionItem;
import com.rem.backend.analytics.reporting.shared.dto.GroupedSummary;
import com.rem.backend.analytics.reporting.shared.dto.KpiCard;
import com.rem.backend.analytics.reporting.shared.dto.ReportingView;
import com.rem.backend.analytics.reporting.shared.enums.ChartType;
import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.enums.Severity;
import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;
import com.rem.backend.analytics.reporting.shared.util.DateRange;
import com.rem.backend.analytics.reporting.shared.util.ReportingDateUtil;
import com.rem.backend.analytics.reporting.shared.util.ReportingFactory;
import com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil;
import com.rem.backend.customermanagement.repository.CustomerAccountRepo;
import com.rem.backend.customermanagement.repository.CustomerRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil.nz;

/**
 * Admin reporting for sales: bookings, customer outstanding, collections and payment schedule.
 * Monetary figures are sourced from accounting (receivable/booking-income accounts) where
 * possible; operational counts come from the business tables.
 */
@Service
@AllArgsConstructor
public class AdminSalesReportService {

    private final AdminBookingReportRepository bookingReportRepo;
    private final AdminAccountingReportRepository accountingRepo;
    private final AdminFinancialReportService financialReportService;
    private final CustomerRepo customerRepo;
    private final CustomerAccountRepo customerAccountRepo;

    public ReportingView bookingsOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);

        long active = bookingReportRepo.countActiveBookings(organizationId);
        long completed = bookingReportRepo.countCompletedBookings(organizationId);
        long cancelled = bookingReportRepo.countCancelledBookings(organizationId);
        long inRange = bookingReportRepo.countActiveBookingsInRange(organizationId, range.getStart(), range.getEnd());
        double bookingValue = nz(bookingReportRepo.totalBookingValue(organizationId));
        double bookingIncome = financialReportService.codeBalance(organizationId, range, JournalUtilities.BOOKING_REVENUE);

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("activeBookings", "Active Bookings", active, "FaBookBookmark", ColorHint.PRIMARY),
                ReportingFactory.countCard("bookingsInPeriod", "Bookings in Period", inRange, "FaCalendarCheck", ColorHint.INFO),
                ReportingFactory.countCard("completedBookings", "Completed Bookings", completed, "FaCircleCheck", ColorHint.SUCCESS),
                ReportingFactory.countCard("cancelledBookings", "Cancelled Bookings", cancelled, "FaCircleXmark", ColorHint.DANGER),
                ReportingFactory.currencyCard("totalBookingValue", "Total Booking Value", bookingValue, "FaSackDollar", ColorHint.PRIMARY),
                ReportingFactory.currencyCard("bookingIncome", "Recognized Booking Income", bookingIncome, "FaArrowTrendUp", ColorHint.SUCCESS)
        );

        GroupedSummary statusBreakdown = GroupedSummary.builder()
                .key("bookingStatus").label("Booking Status Breakdown")
                .total(active + cancelled)
                .items(List.of(
                        ReportingFactory.countValue("active", "Active", active, ColorHint.PRIMARY),
                        ReportingFactory.countValue("completed", "Completed", completed, ColorHint.SUCCESS),
                        ReportingFactory.countValue("cancelled", "Cancelled", cancelled, ColorHint.DANGER)))
                .build();

        return ReportingView.builder()
                .key("bookings-overview").title("Bookings Overview")
                .subtitle("Sales & booking activity")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(statusBreakdown))
                .charts(List.of(monthlyBookingChart(organizationId, range)))
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    public ReportingView customerOutstandingOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        long customers = customerRepo.countByOrganizationId(organizationId);
        double receivableLedger = financialReportService.codeBalance(organizationId, range, JournalUtilities.CUSTOMER_RECEIVABLE);
        double receivableBusiness = nz(customerAccountRepo.getTotalReceiveableAmountByOrganizationId(organizationId));

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("totalCustomers", "Total Customers", customers, "FaUsers", ColorHint.PRIMARY),
                ReportingFactory.currencyCard("receivableLedger", "Receivable (Accounting)", receivableLedger, "FaHandHoldingDollar", ColorHint.WARNING),
                ReportingFactory.currencyCard("receivableBusiness", "Receivable (Customer Ledger)", receivableBusiness, "FaFileInvoiceDollar", ColorHint.INFO)
        );

        List<ExceptionItem> alerts = new ArrayList<>();
        if (Math.abs(receivableLedger - receivableBusiness) > 1d) {
            // Surface a reconciliation hint when the GL receivable diverges from the sub-ledger.
            alerts.add(ExceptionItem.builder()
                    .key("receivableMismatch").severity(Severity.WARNING)
                    .title("Receivable mismatch")
                    .description("Accounting receivable and customer sub-ledger differ; review reconciliation.")
                    .value(ReportingFormatUtil.round2(receivableLedger - receivableBusiness))
                    .formattedValue(ReportingFormatUtil.compact(receivableLedger - receivableBusiness))
                    .colorHint(ColorHint.WARNING)
                    .build());
        }

        return ReportingView.builder()
                .key("customer-outstanding-overview").title("Customer Outstanding Overview")
                .subtitle("Receivables across the organization")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of())
                .charts(List.of())
                .tables(List.of())
                .alerts(alerts)
                .build();
    }

    public ReportingView collectionsOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        double collectedInRange = nz(customerAccountRepo.getTotalReceivedAmountByOrganizationIdAndDate(
                organizationId, range.getStart()));
        double outstanding = financialReportService.codeBalance(organizationId, range, JournalUtilities.CUSTOMER_RECEIVABLE);

        List<KpiCard> cards = List.of(
                ReportingFactory.currencyCard("collectedInPeriod", "Collected (Period)", collectedInRange, "FaMoneyBillWave", ColorHint.SUCCESS),
                ReportingFactory.currencyCard("outstandingReceivable", "Outstanding Receivable", outstanding, "FaHandHoldingDollar", ColorHint.WARNING)
        );

        ChartData collections = financialReportService.revenueVsCollectionChart(organizationId, range);

        return ReportingView.builder()
                .key("collections-overview").title("Collections Overview")
                .subtitle("Customer collections vs booking income")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of())
                .charts(List.of(collections))
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    public ReportingView paymentScheduleOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        double outstanding = financialReportService.codeBalance(organizationId, range, JournalUtilities.CUSTOMER_RECEIVABLE);

        List<KpiCard> cards = List.of(
                ReportingFactory.currencyCard("outstandingReceivable", "Outstanding Receivable", outstanding, "FaHandHoldingDollar", ColorHint.WARNING)
        );

        // TODO(schedule-due-dates): payment_schedule rows currently carry no per-installment due
        // date, and installment CustomerPayment pre-generation is disabled. True "overdue
        // installment" reporting requires either a due-date column on the schedule lines or
        // pre-generated dated installments. Until then we surface the GL outstanding only and
        // flag the limitation so it is not silently misrepresented.
        List<ExceptionItem> alerts = List.of(ExceptionItem.builder()
                .key("overdueUnavailable").severity(Severity.INFO)
                .title("Overdue installments not yet trackable")
                .description("Per-installment due dates are not stored; overdue aging is pending a schema enhancement.")
                .colorHint(ColorHint.INFO)
                .build());

        return ReportingView.builder()
                .key("payment-schedule-overview").title("Payment Schedule Overview")
                .subtitle("Installment outstanding")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of())
                .charts(List.of())
                .tables(List.of())
                .alerts(alerts)
                .build();
    }

    // ---------------------------------------------------------------------

    public ChartData monthlyBookingChart(long organizationId, DateRange range) {
        List<YearMonth> buckets = ReportingDateUtil.monthBuckets(range);
        Map<YearMonth, Double> byMonth = new LinkedHashMap<>();
        for (Map<String, Object> row : bookingReportRepo.monthlyBookingCount(
                organizationId, range.getStart(), range.getEnd())) {
            Integer yr = toInt(row.get("yr"));
            Integer mo = toInt(row.get("monthNo"));
            Double cnt = toDouble(row.get("cnt"));
            if (yr != null && mo != null) {
                byMonth.put(YearMonth.of(yr, mo), cnt);
            }
        }
        List<Object> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (YearMonth ym : buckets) {
            labels.add(ReportingDateUtil.monthLabel(ym));
            values.add(byMonth.getOrDefault(ym, 0d));
        }
        return ChartData.builder()
                .key("monthlyBookings").title("Monthly Bookings").type(ChartType.BAR)
                .labels(labels)
                .series(List.of(ChartSeries.builder().name("Bookings").data(values).colorHint(ColorHint.PRIMARY).build()))
                .build();
    }

    private static Integer toInt(Object o) {
        if (o == null) {
            return null;
        }
        return ((Number) o).intValue();
    }

    private static Double toDouble(Object o) {
        if (o == null) {
            return 0d;
        }
        return ((Number) o).doubleValue();
    }
}
