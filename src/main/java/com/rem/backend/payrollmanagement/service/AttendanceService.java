package com.rem.backend.payrollmanagement.service;

import com.rem.backend.payrollmanagement.dto.AttendanceRequest;
import com.rem.backend.payrollmanagement.entity.Attendance;
import com.rem.backend.payrollmanagement.repository.AttendanceRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public Map<String, Object> markAttendance(AttendanceRequest request) {
        try {
            // Check if attendance already marked for this employee on this date
            Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndAttendanceDate(
                    request.getEmployeeId(), request.getAttendanceDate());

            if (existing.isPresent()) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER,
                        "Attendance already marked for this date. Use update endpoint instead.");
            }

            Attendance attendance = Attendance.builder()
                    .organizationId(request.getOrganizationId())
                    .employeeId(request.getEmployeeId())
                    .attendanceDate(request.getAttendanceDate())
                    .checkInTime(request.getCheckInTime())
                    .checkOutTime(request.getCheckOutTime())
                    .status(request.getStatus() != null ? request.getStatus() : "PRESENT")
                    .hoursWorked(request.getHoursWorked())
                    .remarks(request.getRemarks())
                    .build();

            Attendance saved = attendanceRepository.save(attendance);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> markBulkAttendance(List<AttendanceRequest> requests) {
        try {
            int successCount = 0;
            int skippedCount = 0;

            for (AttendanceRequest request : requests) {
                Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndAttendanceDate(
                        request.getEmployeeId(), request.getAttendanceDate());

                if (existing.isPresent()) {
                    skippedCount++;
                    continue;
                }

                Attendance attendance = Attendance.builder()
                        .organizationId(request.getOrganizationId())
                        .employeeId(request.getEmployeeId())
                        .attendanceDate(request.getAttendanceDate())
                        .checkInTime(request.getCheckInTime())
                        .checkOutTime(request.getCheckOutTime())
                        .status(request.getStatus() != null ? request.getStatus() : "PRESENT")
                        .hoursWorked(request.getHoursWorked())
                        .remarks(request.getRemarks())
                        .build();

                attendanceRepository.save(attendance);
                successCount++;
            }

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("successCount", successCount);
            result.put("skippedCount", skippedCount);

            return ResponseMapper.buildResponse(Responses.SUCCESS, result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> updateAttendance(Long id, AttendanceRequest request) {
        try {
            Optional<Attendance> optional = attendanceRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Attendance record not found");
            }

            Attendance attendance = optional.get();
            if (request.getCheckInTime() != null) attendance.setCheckInTime(request.getCheckInTime());
            if (request.getCheckOutTime() != null) attendance.setCheckOutTime(request.getCheckOutTime());
            if (request.getStatus() != null) attendance.setStatus(request.getStatus());
            if (request.getHoursWorked() != null) attendance.setHoursWorked(request.getHoursWorked());
            if (request.getRemarks() != null) attendance.setRemarks(request.getRemarks());

            Attendance saved = attendanceRepository.save(attendance);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAttendanceByEmployee(Long employeeId, Pageable pageable) {
        try {
            Page<Attendance> records = attendanceRepository.findByEmployeeId(employeeId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, records);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAttendanceByEmployeeAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        try {
            List<Attendance> records = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employeeId, startDate, endDate);
            return ResponseMapper.buildResponse(Responses.SUCCESS, records);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getDailyAttendance(Long organizationId, LocalDate date) {
        try {
            List<Attendance> records = attendanceRepository.findByOrganizationIdAndAttendanceDate(organizationId, date);
            return ResponseMapper.buildResponse(Responses.SUCCESS, records);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAttendanceSummary(Long employeeId, LocalDate startDate, LocalDate endDate) {
        try {
            long present = attendanceRepository.countByEmployeeIdAndStatusAndAttendanceDateBetween(employeeId, "PRESENT", startDate, endDate);
            long absent = attendanceRepository.countByEmployeeIdAndStatusAndAttendanceDateBetween(employeeId, "ABSENT", startDate, endDate);
            long halfDay = attendanceRepository.countByEmployeeIdAndStatusAndAttendanceDateBetween(employeeId, "HALF_DAY", startDate, endDate);
            long late = attendanceRepository.countByEmployeeIdAndStatusAndAttendanceDateBetween(employeeId, "LATE", startDate, endDate);
            long onLeave = attendanceRepository.countByEmployeeIdAndStatusAndAttendanceDateBetween(employeeId, "ON_LEAVE", startDate, endDate);

            Map<String, Object> summary = new java.util.HashMap<>();
            summary.put("present", present);
            summary.put("absent", absent);
            summary.put("halfDay", halfDay);
            summary.put("late", late);
            summary.put("onLeave", onLeave);
            summary.put("totalWorkingDays", present + absent + halfDay + late + onLeave);

            return ResponseMapper.buildResponse(Responses.SUCCESS, summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> deleteAttendance(Long id) {
        try {
            if (!attendanceRepository.existsById(id)) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Attendance record not found");
            }
            attendanceRepository.deleteById(id);
            return ResponseMapper.buildResponse(Responses.SUCCESS, "Attendance record deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
