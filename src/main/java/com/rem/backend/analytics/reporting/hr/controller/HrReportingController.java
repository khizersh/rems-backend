package com.rem.backend.analytics.reporting.hr.controller;

import com.rem.backend.analytics.reporting.hr.service.HrAttendanceReportService;
import com.rem.backend.analytics.reporting.hr.service.HrDashboardService;
import com.rem.backend.analytics.reporting.hr.service.HrPayrollReportService;
import com.rem.backend.analytics.reporting.hr.service.HrReportContextService;
import com.rem.backend.analytics.reporting.hr.service.HrWorkforceReportService;
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
 * HR reporting API — a workforce/payroll-focused subset of the reporting platform scoped to
 * HR-authorized roles. Every endpoint:
 * <ul>
 *   <li>resolves the tenant organization from the authenticated user (never from the client),</li>
 *   <li>verifies the caller holds an HR-authorized role (see {@link HrReportContextService}),</li>
 *   <li>binds the shared {@link ReportingFilter} from query params, and</li>
 *   <li>returns the uniform {@link ReportingView} envelope in the standard response wrapper.</li>
 * </ul>
 *
 * <p>Filters are optional query params, e.g.:
 * {@code GET /api/reporting/hr/payroll-overview?year=2026&month=6}.
 */
@RestController
@RequestMapping("/api/reporting/hr")
@AllArgsConstructor
public class HrReportingController {

    private final HrReportContextService contextService;
    private final HrDashboardService dashboardService;
    private final HrWorkforceReportService workforceReportService;
    private final HrAttendanceReportService attendanceReportService;
    private final HrPayrollReportService payrollReportService;

    // ----- Dashboard --------------------------------------------------------

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> dashboardService.buildDashboard(orgId, filter));
    }

    // ----- Workforce ---------------------------------------------------------

    @GetMapping("/employee-overview")
    public Map<String, Object> employeeOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> workforceReportService.employeeOverview(orgId, filter));
    }

    @GetMapping("/department-overview")
    public Map<String, Object> departmentOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> workforceReportService.departmentOverview(orgId, filter));
    }

    // ----- Attendance / leave -------------------------------------------------

    @GetMapping("/attendance-overview")
    public Map<String, Object> attendanceOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> attendanceReportService.attendanceOverview(orgId, filter));
    }

    @GetMapping("/leave-overview")
    public Map<String, Object> leaveOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> attendanceReportService.leaveOverview(orgId, filter));
    }

    // ----- Payroll / compensation ---------------------------------------------

    @GetMapping("/payroll-overview")
    public Map<String, Object> payrollOverview(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> payrollReportService.payrollOverview(orgId, filter));
    }

    @GetMapping("/compensation-breakdown")
    public Map<String, Object> compensationBreakdown(ReportingFilter filter, HttpServletRequest request) {
        return run(request, orgId -> payrollReportService.compensationBreakdown(orgId, filter));
    }

    // ----- Shared execution wrapper ----------------------------------------

    /**
     * Resolves the org from the JWT principal (with HR role enforcement), invokes the report
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
