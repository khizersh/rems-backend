package com.rem.backend.payrollmanagement.controller;

import com.rem.backend.payrollmanagement.dto.LeaveRequestDTO;
import com.rem.backend.payrollmanagement.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hr/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping("/apply")
    public Map<String, Object> applyLeave(@RequestBody LeaveRequestDTO request) {
        return leaveService.applyLeave(request);
    }

    @PutMapping("/approve/{id}")
    public Map<String, Object> approveLeave(@PathVariable Long id, @RequestParam String approvedBy) {
        return leaveService.approveLeave(id, approvedBy);
    }

    @PutMapping("/reject/{id}")
    public Map<String, Object> rejectLeave(@PathVariable Long id, @RequestParam String rejectedBy) {
        return leaveService.rejectLeave(id, rejectedBy);
    }

    @PutMapping("/cancel/{id}")
    public Map<String, Object> cancelLeave(@PathVariable Long id) {
        return leaveService.cancelLeave(id);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getLeaveById(@PathVariable Long id) {
        return leaveService.getLeaveById(id);
    }

    @GetMapping("/employee/{employeeId}")
    public Map<String, Object> getLeavesByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return leaveService.getLeavesByEmployee(employeeId, pageable);
    }

    @GetMapping("/organization/{organizationId}")
    public Map<String, Object> getLeavesByOrganization(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return leaveService.getLeavesByOrganization(organizationId, pageable);
    }

    @GetMapping("/pending/{organizationId}")
    public Map<String, Object> getPendingLeaves(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return leaveService.getPendingLeaves(organizationId, pageable);
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteLeave(@PathVariable Long id) {
        return leaveService.deleteLeave(id);
    }
}
