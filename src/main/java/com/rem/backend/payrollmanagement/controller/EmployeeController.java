package com.rem.backend.payrollmanagement.controller;

import com.rem.backend.payrollmanagement.dto.EmployeeRequest;
import com.rem.backend.payrollmanagement.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hr/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping("/create")
    public Map<String, Object> createEmployee(@RequestBody EmployeeRequest request) {
        return employeeService.createEmployee(request);
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateEmployee(@PathVariable Long id, @RequestBody EmployeeRequest request) {
        return employeeService.updateEmployee(id, request);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    @GetMapping("/organization/{organizationId}")
    public Map<String, Object> getEmployeesByOrganization(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return employeeService.getEmployeesByOrganization(organizationId, pageable);
    }

    @GetMapping("/department/{organizationId}/{departmentId}")
    public Map<String, Object> getEmployeesByDepartment(
            @PathVariable Long organizationId,
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return employeeService.getEmployeesByDepartment(organizationId, departmentId, pageable);
    }

    @GetMapping("/search/{organizationId}")
    public Map<String, Object> searchEmployees(
            @PathVariable Long organizationId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return employeeService.searchEmployees(organizationId, keyword, pageable);
    }

    @PutMapping("/terminate/{id}")
    public Map<String, Object> terminateEmployee(@PathVariable Long id) {
        return employeeService.terminateEmployee(id);
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteEmployee(@PathVariable Long id) {
        return employeeService.deleteEmployee(id);
    }
}
