package com.rem.backend.analytics.reporting.admin.service;

import com.rem.backend.accountingmanagement.utility.JournalUtilities;
import com.rem.backend.analytics.reporting.admin.query.AdminBookingReportRepository;
// Note: JournalUtilities is kept for CUSTOMER_RECEIVABLE and BOOKING_REVENUE single-code lookups.
import com.rem.backend.analytics.reporting.admin.query.AdminInventoryReportRepository;
import com.rem.backend.analytics.reporting.admin.query.AdminWorkforceReportRepository;
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
import com.rem.backend.customermanagement.repository.CustomerAccountRepo;
import com.rem.backend.customermanagement.repository.CustomerRepo;
import com.rem.backend.payrollmanagement.entity.SalarySlip;
import com.rem.backend.payrollmanagement.repository.EmployeeRepository;
import com.rem.backend.payrollmanagement.repository.SalarySlipRepository;
import com.rem.backend.projectmanagement.repository.ProjectRepo;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrder;
import com.rem.backend.purchasemanagement.enums.PoStatus;
import com.rem.backend.purchasemanagement.repository.PurchaseOrderRepo;
import com.rem.backend.vendormanagement.repository.VendorAccountRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil.nz;

/**
 * Composes the executive admin dashboard: cross-module KPI cards, an accounting-derived
 * financial summary, operational snapshots, trend charts and alerts &mdash; all in one payload.
 * <p>
 * This service is a <em>composition</em> layer: it delegates money figures to the financial
 * service (accounting source of truth) and pulls operational counts from the business modules.
 */
@Service
@AllArgsConstructor
public class AdminDashboardService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final AdminFinancialReportService financialReportService;
    private final AdminSalesReportService salesReportService;
    private final AdminBookingReportRepository bookingReportRepo;
    private final AdminInventoryReportRepository inventoryReportRepo;
    private final AdminWorkforceReportRepository workforceReportRepo;
    private final CustomerRepo customerRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final VendorAccountRepo vendorAccountRepo;
    private final ProjectRepo projectRepo;
    private final EmployeeRepository employeeRepository;
    private final SalarySlipRepository salarySlipRepository;
    private final PurchaseOrderRepo purchaseOrderRepo;

    public ReportingView buildDashboard(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);

        // --- Operational counts -------------------------------------------------
        long activeBookings = bookingReportRepo.countActiveBookings(organizationId);
        long completedBookings = bookingReportRepo.countCompletedBookings(organizationId);
        long cancelledBookings = bookingReportRepo.countCancelledBookings(organizationId);
        double bookingValue = nz(bookingReportRepo.totalBookingValue(organizationId));

        Map<String, Object> inv = inventoryReportRepo.inventorySnapshot(organizationId);
        double availableUnits = num(inv, "availableUnits");
        double bookedUnits = num(inv, "bookedUnits");

        long customers = customerRepo.countByOrganizationId(organizationId);
        long vendors = vendorAccountRepo.findAllByOrgId(organizationId).size();
        long projects = projectRepo.countByOrganizationId(organizationId);
        long employees = employeeRepository.countByOrganizationId(organizationId);
        long activeEmployees = employeeRepository.countByOrganizationIdAndStatus(organizationId, STATUS_ACTIVE);
        long users = workforceReportRepo.countActiveUsers(organizationId);

        double purchaseVolume = purchaseVolume(organizationId);
        double payrollMonth = currentMonthPayroll(organizationId);

        // --- Money figures (accounting source of truth) -------------------------
        // Total receivables = Customer Receivable COA (single receivable account in current COA)
        double receivables = financialReportService.codeBalance(
                organizationId, range, JournalUtilities.CUSTOMER_RECEIVABLE);

        double bookingIncome = financialReportService.codeBalance(
                organizationId, range, JournalUtilities.BOOKING_REVENUE);
        double collectionsPeriod = nz(customerAccountRepo.getTotalReceivedAmountByOrganizationIdAndDate(
                organizationId, range.getStart()));

        double assets       = financialReportService.typeBalance(organizationId, range, "ASSET");
        // Total payables = ALL liability accounts through the full 5-level COA hierarchy
        // (account_type → account_category → account_group → chart_of_account → journal_detail_entry)
        double totalPayables       = financialReportService.typeBalance(organizationId, range, "LIABILITY");
        double currentLiabilities  = financialReportService.categoryBalance(organizationId, range, "LIABILITY", "CURRENT");
        double longTermLiabilities = financialReportService.categoryBalance(organizationId, range, "LIABILITY", "LONG TERM");

        double income    = financialReportService.typeBalance(organizationId, range, "INCOME");
        double expense   = financialReportService.typeBalance(organizationId, range, "EXPENSE");
        double netProfit = income - expense;

        // --- A. Business summary cards -----------------------------------------
        List<KpiCard> cards = new ArrayList<>();
        cards.add(ReportingFactory.countCard("activeBookings", "Active Bookings", activeBookings, "FaBookBookmark", ColorHint.PRIMARY));
        cards.add(ReportingFactory.currencyCard("totalBookingValue", "Total Booking Value", bookingValue, "FaSackDollar", ColorHint.PRIMARY));
        cards.add(ReportingFactory.currencyCard("totalReceivables", "Total Receivables", receivables, "FaHandHoldingDollar", ColorHint.WARNING));
        cards.add(ReportingFactory.currencyCard("collectionsPeriod", "Collections (Period)", collectionsPeriod, "FaMoneyBillWave", ColorHint.SUCCESS));
        cards.add(ReportingFactory.currencyCard("totalPayables", "Total Payables", totalPayables, "FaFileInvoiceDollar", ColorHint.DANGER));
        cards.add(ReportingFactory.currencyCard("bookingIncome", "Booking Income", bookingIncome, "FaArrowTrendUp", ColorHint.SUCCESS));
        cards.add(ReportingFactory.countCard("availableInventory", "Available Units", availableUnits, "FaDoorOpen", ColorHint.SUCCESS));
        cards.add(ReportingFactory.countCard("bookedInventory", "Booked / Reserved Units", bookedUnits, "FaDoorClosed", ColorHint.WARNING));
        cards.add(ReportingFactory.countCard("activeVendors", "Active Vendors", vendors, "FaPeopleCarryBox", ColorHint.INFO));
        cards.add(ReportingFactory.currencyCard("purchaseVolume", "Purchase Volume", purchaseVolume, "FaTruckRampBox", ColorHint.INFO));
        cards.add(ReportingFactory.currencyCard("payrollCost", "Payroll (This Month)", payrollMonth, "FaMoneyCheckDollar", ColorHint.WARNING));
        cards.add(ReportingFactory.countCard("totalEmployees", "Employees", employees, "FaUsers", ColorHint.PRIMARY));
        cards.add(ReportingFactory.countCard("totalCustomers", "Customers", customers, "FaUserGroup", ColorHint.PRIMARY));
        cards.add(ReportingFactory.countCard("totalProjects", "Projects", projects, "FaDiagramProject", ColorHint.PRIMARY));
        cards.add(ReportingFactory.countCard("totalUsers", "System Users", users, "FaUserGear", ColorHint.NEUTRAL));

        // --- B. Financial summary (accounting) ---------------------------------
        // Liabilities are split into Current and Long-Term for a complete picture
        GroupedSummary financialSummary = GroupedSummary.builder()
                .key("financialSummary").label("Financial Summary")
                .total(ReportingFormatUtil.round2(netProfit))
                .formattedTotal(ReportingFormatUtil.compact(netProfit))
                .items(List.of(
                        ReportingFactory.currencyValue("assets",             "Assets",               assets,             ColorHint.PRIMARY),
                        ReportingFactory.currencyValue("currentLiabilities", "Current Liabilities",  currentLiabilities, ColorHint.DANGER),
                        ReportingFactory.currencyValue("longTermLiabilities","Long-Term Liabilities", longTermLiabilities, ColorHint.WARNING),
                        ReportingFactory.currencyValue("income",             "Income",               income,             ColorHint.SUCCESS),
                        ReportingFactory.currencyValue("expense",            "Expense",              expense,            ColorHint.DANGER),
                        ReportingFactory.currencyValue("netProfit",          "Net Profit",           netProfit,
                                netProfit >= 0 ? ColorHint.SUCCESS : ColorHint.DANGER)))
                .build();

        // --- D. Operational snapshots ------------------------------------------
        GroupedSummary bookingStatus = GroupedSummary.builder()
                .key("bookingStatus").label("Booking Status")
                .total(activeBookings + cancelledBookings)
                .items(List.of(
                        ReportingFactory.countValue("active", "Active", activeBookings, ColorHint.PRIMARY),
                        ReportingFactory.countValue("completed", "Completed", completedBookings, ColorHint.SUCCESS),
                        ReportingFactory.countValue("cancelled", "Cancelled", cancelledBookings, ColorHint.DANGER)))
                .build();

        GroupedSummary unitStatus = GroupedSummary.builder()
                .key("unitAvailability").label("Unit Availability")
                .total(availableUnits + bookedUnits)
                .items(List.of(
                        ReportingFactory.countValue("available", "Available", availableUnits, ColorHint.SUCCESS),
                        ReportingFactory.countValue("booked", "Booked / Reserved", bookedUnits, ColorHint.WARNING)))
                .build();

        GroupedSummary workforce = GroupedSummary.builder()
                .key("workforce").label("Workforce")
                .total(employees)
                .items(List.of(
                        ReportingFactory.countValue("active", "Active", activeEmployees, ColorHint.SUCCESS),
                        ReportingFactory.countValue("other", "Other", Math.max(0, employees - activeEmployees), ColorHint.NEUTRAL)))
                .build();

        // --- C. Charts ----------------------------------------------------------
        List<ChartData> charts = List.of(
                financialReportService.revenueVsCollectionChart(organizationId, range),
                salesReportService.monthlyBookingChart(organizationId, range)
        );

        // --- E. Alerts / exceptions --------------------------------------------
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
        if (totalPayables > 0 && assets > 0 && totalPayables > assets) {
            alerts.add(ExceptionItem.builder()
                    .key("payableExceedsAssets").severity(Severity.CRITICAL)
                    .title("Total payables exceed asset balance")
                    .description("Total liabilities (current + long-term) exceed the total asset balance.")
                    .value(ReportingFormatUtil.round2(totalPayables))
                    .formattedValue(ReportingFormatUtil.compact(totalPayables))
                    .colorHint(ColorHint.DANGER)
                    .build());
        }

        return ReportingView.builder()
                .key("admin-dashboard")
                .title("Admin Dashboard")
                .subtitle("Executive cross-module overview")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(financialSummary, bookingStatus, unitStatus, workforce))
                .charts(charts)
                .tables(List.of())
                .alerts(alerts)
                .build();
    }

    private double purchaseVolume(long organizationId) {
        double volume = 0d;
        for (PoStatus status : PoStatus.values()) {
            for (PurchaseOrder po : purchaseOrderRepo.findByOrgIdAndStatus(organizationId, status)) {
                volume += nz(po.getTotalAmount());
            }
        }
        return volume;
    }

    private double currentMonthPayroll(long organizationId) {
        LocalDate now = LocalDate.now();
        List<SalarySlip> slips = salarySlipRepository.findByOrganizationIdAndSalaryMonthAndSalaryYear(
                organizationId, now.getMonthValue(), now.getYear());
        BigDecimal sum = BigDecimal.ZERO;
        for (SalarySlip slip : slips) {
            if (slip.getNetSalary() != null) {
                sum = sum.add(slip.getNetSalary());
            }
        }
        return sum.doubleValue();
    }

    private static double num(Map<String, Object> row, String key) {
        if (row == null) {
            return 0d;
        }
        return nz((Number) row.get(key));
    }
}
