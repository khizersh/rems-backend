package com.rem.backend.payrollmanagement.controller;

import com.rem.backend.payrollmanagement.dto.DepartmentRequest;
import com.rem.backend.payrollmanagement.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hr/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping("/create")
    public Map<String, Object> createDepartment(@RequestBody DepartmentRequest request) {
        return departmentService.createDepartment(request);
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateDepartment(@PathVariable Long id, @RequestBody DepartmentRequest request) {
        return departmentService.updateDepartment(id, request);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getDepartmentById(@PathVariable Long id) {
        return departmentService.getDepartmentById(id);
    }

    @GetMapping("/organization/{organizationId}")
    public Map<String, Object> getDepartmentsByOrganization(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return departmentService.getDepartmentsByOrganization(organizationId, pageable);
    }

    @GetMapping("/active/{organizationId}")
    public Map<String, Object> getActiveDepartments(@PathVariable Long organizationId) {
        return departmentService.getActiveDepartments(organizationId);
    }

    @GetMapping("/search/{organizationId}")
    public Map<String, Object> searchDepartments(
            @PathVariable Long organizationId,
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return departmentService.searchDepartments(organizationId, name, pageable);
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteDepartment(@PathVariable Long id) {
        return departmentService.deleteDepartment(id);
    }
}
