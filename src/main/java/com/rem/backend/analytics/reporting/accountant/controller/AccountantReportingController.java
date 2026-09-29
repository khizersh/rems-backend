package com.rem.backend.analytics.reporting.accountant.controller;

import com.rem.backend.analytics.reporting.accountant.service.AccountantDashboardService;
import com.rem.backend.analytics.reporting.accountant.service.AccountantReportContextService;
import com.rem.backend.analytics.reporting.admin.service.AdminFinancialReportService;
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
 * Accountant reporting API — a finance-focused subset of the reporting platform scoped to the
 * {@code ACCOUNTANT_ROLE}. Every endpoint:
 * <ul>
 *   <li>resolves the tenant organization from the authenticated user (never from the client),</li>
 *   <li>verifies the caller holds a finance-authorized role (see
 *       {@link AccountantReportContextService}),</li>
 *   <li>binds the shared {@link ReportingFilter} from query params, and</li>
 *   <li>returns the uniform {@link ReportingView} envelope in the standard response wrapper.</li>
 * </ul>
 *
 * <p>The financial reports themselves are org-scoped and role-agnostic, so they are reused from
 * {@link AdminFinancialReportService} / {@link AdminSalesReportService} rather than duplicated.
 */
@RestController
@RequestMapping("/api/reporting/accountant")
@AllArgsConstructor
public class AccountantReportingController {

    private final AccountantReportContextService contextService;
    private final AccountantDashboardService dashboardService;
    private final AdminFinancialReportService financialReportService;
    private final AdminSalesReportService salesReportService;

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

    // ----- Collections (finance-relevant sales data) -----------------------

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

    // ----- Shared execution wrapper ----------------------------------------

    /**
     * Resolves the org from the JWT principal (with role enforcement), invokes the report
     * function and wraps the result (or error) in the standard response envelope.
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
