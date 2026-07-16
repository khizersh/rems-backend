package com.rem.backend.analytics.reporting.admin.controller;

import com.rem.backend.analytics.reporting.admin.service.AdminDashboardService;
import com.rem.backend.analytics.reporting.admin.service.AdminFinancialReportService;
import com.rem.backend.analytics.reporting.admin.service.AdminHrReportService;
import com.rem.backend.analytics.reporting.admin.service.AdminProcurementReportService;
import com.rem.backend.analytics.reporting.admin.service.AdminProjectPropertyReportService;
import com.rem.backend.analytics.reporting.admin.service.AdminReportContextService;
import com.rem.backend.analytics.reporting.admin.service.AdminSalesReportService;
import com.rem.backend.analytics.reporting.shared.dto.ReportingView;
import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.LongFunction;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

/**
 * Admin reporting API. Every endpoint:
 * <ul>
 *   <li>derives the tenant organization from the authenticated user (never from the client),</li>
 *   <li>binds the shared {@link ReportingFilter} from query params,</li>
 *   <li>returns the uniform {@link ReportingView} envelope wrapped in the standard
 *       {@code responseCode/responseMessage/data} response.</li>
 * </ul>
 *
 * <p>Filters are optional query params, e.g.:
 * {@code GET /api/reporting/admin/financial-overview?fromDate=1-1-2026&toDate=30-6-2026}.
 */
@RestController
@RequestMapping("/api/reporting/admin")
@AllArgsConstructor
public class AdminReportingController {

    private final AdminReportContextService contextService;
    private final AdminDashboardService dashboardService;
    private final AdminFinancialReportService financialReportService;
    private final AdminSalesReportService salesReportService;
    private final AdminProjectPropertyReportService projectPropertyReportService;
    private final AdminProcurementReportService procurementReportService;
    private final AdminHrReportService hrReportService;

    // ----- Dashboard --------------------------------------------------------

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> dashboardService.buildDashboard(orgId, filter));
    }

    // ----- Financial (accounting-backed) -----------------------------------

    @GetMapping("/financial-overview")
    public Map<String, Object> financialOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> financialReportService.financialOverview(orgId, filter));
    }

    @GetMapping("/financial-overview/by-type")
    public Map<String, Object> financialOverviewByType(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> financialReportService.financialOverviewByType(orgId, filter));
    }

    @GetMapping("/financial-overview/by-category")
    public Map<String, Object> financialOverviewByCategory(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> financialReportService.financialOverviewByCategory(orgId, filter));
    }

    @GetMapping("/financial-overview/by-account")
    public Map<String, Object> financialOverviewByAccount(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> financialReportService.financialOverviewByAccount(orgId, filter));
    }

    @GetMapping("/revenue-summary")
    public Map<String, Object> revenueSummary(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> financialReportService.revenueSummary(orgId, filter));
    }

    @GetMapping("/expense-summary")
    public Map<String, Object> expenseSummary(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> financialReportService.expenseSummary(orgId, filter));
    }

    @GetMapping("/receivable-summary")
    public Map<String, Object> receivableSummary(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> financialReportService.receivableSummary(orgId, filter));
    }

    @GetMapping("/payable-summary")
    public Map<String, Object> payableSummary(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> financialReportService.payableSummary(orgId, filter));
    }

    @GetMapping("/accounting-trial-summary")
    public Map<String, Object> accountingTrialSummary(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> financialReportService.accountingTrialSummary(orgId, filter));
    }

    // ----- Bookings / customers / collections ------------------------------

    @GetMapping("/bookings-overview")
    public Map<String, Object> bookingsOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> salesReportService.bookingsOverview(orgId, filter));
    }

    @GetMapping("/customer-outstanding-overview")
    public Map<String, Object> customerOutstandingOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> salesReportService.customerOutstandingOverview(orgId, filter));
    }

    @GetMapping("/collections-overview")
    public Map<String, Object> collectionsOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> salesReportService.collectionsOverview(orgId, filter));
    }

    @GetMapping("/payment-schedule-overview")
    public Map<String, Object> paymentScheduleOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> salesReportService.paymentScheduleOverview(orgId, filter));
    }

    // ----- Projects / property / inventory ---------------------------------

    @GetMapping("/projects-overview")
    public Map<String, Object> projectsOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> projectPropertyReportService.projectsOverview(orgId, filter));
    }

    @GetMapping("/property-overview")
    public Map<String, Object> propertyOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> projectPropertyReportService.propertyOverview(orgId, filter));
    }

    @GetMapping("/inventory-overview")
    public Map<String, Object> inventoryOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> projectPropertyReportService.inventoryOverview(orgId, filter));
    }

    // ----- Procurement / vendor / warehouse --------------------------------

    @GetMapping("/purchase-overview")
    public Map<String, Object> purchaseOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> procurementReportService.purchaseOverview(orgId, filter));
    }

    @GetMapping("/vendor-overview")
    public Map<String, Object> vendorOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> procurementReportService.vendorOverview(orgId, filter));
    }

    @GetMapping("/warehouse-overview")
    public Map<String, Object> warehouseOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> procurementReportService.warehouseOverview(orgId, filter));
    }

    // ----- HR / users -------------------------------------------------------

    @GetMapping("/hr-overview")
    public Map<String, Object> hrOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> hrReportService.hrOverview(orgId, filter));
    }

    @GetMapping("/user-overview")
    public Map<String, Object> userOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> hrReportService.userOverview(orgId, filter));
    }

    // ----- Shared execution wrapper ----------------------------------------

    /**
     * Resolves the org from the JWT principal, invokes the report function and wraps the
     * result (or error) in the standard response envelope used across the codebase.
     */
    private Map<String, Object> run(HttpServletRequest request, LongFunction<ReportingView> reportFn) {
        try {
            String username = (String) request.getAttribute(LOGGED_IN_USER);
            long organizationId = contextService.resolveOrganizationId(username);
            ReportingView view = reportFn.apply(organizationId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, view);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
