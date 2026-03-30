package com.rem.backend.payrollmanagement.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestDTO {
    private Long organizationId;
    private Long employeeId;
    private String leaveType; // ANNUAL, SICK, CASUAL, MATERNITY, PATERNITY, UNPAID
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
