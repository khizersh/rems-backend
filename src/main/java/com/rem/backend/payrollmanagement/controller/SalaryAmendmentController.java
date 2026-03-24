package com.rem.backend.payrollmanagement.controller;

import com.rem.backend.payrollmanagement.dto.SalaryAmendmentRequest;
import com.rem.backend.payrollmanagement.service.SalaryAmendmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hr/salary-amendment")
@RequiredArgsConstructor
public class SalaryAmendmentController {

    private final SalaryAmendmentService salaryAmendmentService;

    @PostMapping("/create")
    public Map<String, Object> createAmendment(@RequestBody SalaryAmendmentRequest request) {
        return salaryAmendmentService.createAmendment(request);
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateAmendment(@PathVariable Long id, @RequestBody SalaryAmendmentRequest request) {
        return salaryAmendmentService.updateAmendment(id, request);
    }

    @GetMapping("/employee/{employeeId}")
    public Map<String, Object> getAmendmentsByEmployee(@PathVariable Long employeeId) {
        return salaryAmendmentService.getAmendmentsByEmployee(employeeId);
    }

    @GetMapping("/organization/{organizationId}")
    public Map<String, Object> getAmendmentsByOrganizationAndMonth(
            @PathVariable Long organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        return salaryAmendmentService.getAmendmentsByOrganizationAndMonth(organizationId, month, year);
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteAmendment(@PathVariable Long id) {
        return salaryAmendmentService.deleteAmendment(id);
    }
}
