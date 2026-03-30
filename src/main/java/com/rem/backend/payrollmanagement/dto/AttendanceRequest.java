package com.rem.backend.payrollmanagement.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceRequest {
    private Long organizationId;
    private Long employeeId;
    private LocalDate attendanceDate;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String status; // PRESENT, ABSENT, HALF_DAY, LATE, ON_LEAVE
    private Double hoursWorked;
    private String remarks;
}
