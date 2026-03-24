package com.rem.backend.payrollmanagement.controller;

import com.rem.backend.payrollmanagement.dto.AttendanceRequest;
import com.rem.backend.payrollmanagement.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/mark")
    public Map<String, Object> markAttendance(@RequestBody AttendanceRequest request) {
        return attendanceService.markAttendance(request);
    }

    @PostMapping("/mark-bulk")
    public Map<String, Object> markBulkAttendance(@RequestBody List<AttendanceRequest> requests) {
        return attendanceService.markBulkAttendance(requests);
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateAttendance(@PathVariable Long id, @RequestBody AttendanceRequest request) {
        return attendanceService.updateAttendance(id, request);
    }

    @GetMapping("/employee/{employeeId}")
    public Map<String, Object> getAttendanceByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "attendanceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return attendanceService.getAttendanceByEmployee(employeeId, pageable);
    }

    @GetMapping("/employee/{employeeId}/range")
    public Map<String, Object> getAttendanceByRange(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return attendanceService.getAttendanceByEmployeeAndDateRange(employeeId, startDate, endDate);
    }

    @GetMapping("/daily/{organizationId}")
    public Map<String, Object> getDailyAttendance(
            @PathVariable Long organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.getDailyAttendance(organizationId, date);
    }

    @GetMapping("/summary/{employeeId}")
    public Map<String, Object> getAttendanceSummary(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return attendanceService.getAttendanceSummary(employeeId, startDate, endDate);
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteAttendance(@PathVariable Long id) {
        return attendanceService.deleteAttendance(id);
    }
}
