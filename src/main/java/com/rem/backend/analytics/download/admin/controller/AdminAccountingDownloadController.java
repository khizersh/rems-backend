package com.rem.backend.analytics.download.admin.controller;

import com.rem.backend.analytics.download.admin.service.AdminAccountingDownloadService;
import com.rem.backend.analytics.download.shared.dto.FinancialStatementReport;
import com.rem.backend.analytics.download.shared.enums.ExportFormat;
import com.rem.backend.analytics.download.shared.export.CsvReportExporter;
import com.rem.backend.analytics.download.shared.filters.DownloadFilter;
import com.rem.backend.analytics.reporting.admin.service.AdminReportContextService;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.BiFunction;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

/**
 * Downloadable accounting statement API. JSON responses use the standard envelope;
 * CSV requests return a file attachment.
 */
@RestController
@RequestMapping("/api/reporting/admin/downloads")
@AllArgsConstructor
public class AdminAccountingDownloadController {

    private final AdminReportContextService contextService;
    private final AdminAccountingDownloadService downloadService;

    @GetMapping("/catalog")
    public Map<String, Object> catalog(HttpServletRequest request) {
        return runJson(request, (orgId, filter) -> downloadService.catalog());
    }

    @GetMapping("/trial-balance")
    public ResponseEntity<?> trialBalance(DownloadFilter filter, HttpServletRequest request) {
        return run(request, filter, downloadService::trialBalance);
    }

    @GetMapping("/profit-loss")
    public ResponseEntity<?> profitLoss(DownloadFilter filter, HttpServletRequest request) {
        return run(request, filter, downloadService::profitLoss);
    }

    @GetMapping("/balance-sheet")
    public ResponseEntity<?> balanceSheet(DownloadFilter filter, HttpServletRequest request) {
        return run(request, filter, downloadService::balanceSheet);
    }

    @GetMapping("/general-ledger")
    public ResponseEntity<?> generalLedger(DownloadFilter filter, HttpServletRequest request) {
        return run(request, filter, downloadService::generalLedger);
    }

    @GetMapping("/journal-register")
    public ResponseEntity<?> journalRegister(DownloadFilter filter, HttpServletRequest request) {
        return run(request, filter, downloadService::journalRegister);
    }

    @GetMapping("/account-statement")
    public ResponseEntity<?> accountStatement(DownloadFilter filter, HttpServletRequest request) {
        return run(request, filter, downloadService::accountStatement);
    }

    private ResponseEntity<?> run(
            HttpServletRequest request,
            DownloadFilter filter,
            BiFunction<Long, DownloadFilter, FinancialStatementReport> reportFn) {
        try {
            String username = (String) request.getAttribute(LOGGED_IN_USER);
            long organizationId = contextService.resolveOrganizationId(username);
            FinancialStatementReport report = reportFn.apply(organizationId, filter);

            ExportFormat format = ExportFormat.fromString(filter != null ? filter.getFormat() : null);
            if (format == ExportFormat.CSV) {
                byte[] csv = CsvReportExporter.toCsv(report);
                String filename = CsvReportExporter.filename(report);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                        .body(csv);
            }

            return ResponseEntity.ok(ResponseMapper.buildResponse(Responses.SUCCESS, report));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage()));
        }
    }

    private Map<String, Object> runJson(
            HttpServletRequest request,
            BiFunction<Long, DownloadFilter, Object> fn) {
        try {
            String username = (String) request.getAttribute(LOGGED_IN_USER);
            long organizationId = contextService.resolveOrganizationId(username);
            return ResponseMapper.buildResponse(Responses.SUCCESS, fn.apply(organizationId, null));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
