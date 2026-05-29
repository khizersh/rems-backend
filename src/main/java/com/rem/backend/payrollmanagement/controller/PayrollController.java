package com.rem.backend.payrollmanagement.controller;

import com.rem.backend.payrollmanagement.dto.ProcessPayrollRequest;
import com.rem.backend.payrollmanagement.service.PayrollService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/hr/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    // ======================== DASHBOARD ========================

    @GetMapping("/dashboard/{organizationId}")
    public Map<String, Object> getDashboard(@PathVariable Long organizationId) {
        return payrollService.getDashboard(organizationId);
    }

    @GetMapping("/department-salary-summary/{organizationId}")
    public Map<String, Object> getDepartmentSalarySummary(@PathVariable Long organizationId) {
        return payrollService.getDepartmentSalarySummary(organizationId);
    }

    // ======================== PAYROLL PROCESSING ========================

    @PostMapping("/process")
    public Map<String, Object> processPayroll(@RequestBody ProcessPayrollRequest requestBody, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);

        return payrollService.processPayroll(requestBody , loggedInUser);
    }

    @PostMapping("/generate-slip/{employeeId}")
    public Map<String, Object> generateSingleSalarySlip(
            @PathVariable Long employeeId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        return payrollService.generateSingleSalarySlip(employeeId, month, year);
    }

    // ======================== SALARY SLIPS ========================

    @GetMapping("/salary-slip/{id}")
    public Map<String, Object> getSalarySlipById(@PathVariable Long id) {
        return payrollService.getSalarySlipById(id);
    }

    @GetMapping("/salary-slips/employee/{employeeId}")
    public Map<String, Object> getSalarySlipsByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "salaryYear") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return payrollService.getSalarySlipsByEmployee(employeeId, pageable);
    }

    @GetMapping("/salary-slips/organization/{organizationId}")
    public Map<String, Object> getSalarySlipsByOrganizationAndMonth(
            @PathVariable Long organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        return payrollService.getSalarySlipsByOrgAndMonth(organizationId, month, year);
    }

    // ======================== PAYMENT STATUS ========================

    @PutMapping("/salary-slip/mark-paid/{id}")
    public Map<String, Object> markSalarySlipPaid(
            @PathVariable Long id,
            @RequestParam Long organizationAccountId,
            @RequestParam String paidBy) {
        return payrollService.markSalarySlipPaid(id, organizationAccountId, paidBy);
    }

    @PutMapping("/salary-slips/mark-all-paid/{organizationId}")
    public Map<String, Object> markAllSlipsPaid(
            @PathVariable Long organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam Long organizationAccountId,
            @RequestParam String paidBy) {
        return payrollService.markAllSlipsPaid(organizationId, month, year, organizationAccountId, paidBy);
    }

    // ======================== PAYROLL HISTORY ========================

    @GetMapping("/history/{organizationId}")
    public Map<String, Object> getPayrollHistory(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return payrollService.getPayrollHistory(organizationId, pageable);
    }

    @PutMapping("/cancel/{payrollId}")
    public Map<String, Object> cancelPayroll(@PathVariable Long payrollId) {
        return payrollService.cancelPayroll(payrollId);
    }
}
