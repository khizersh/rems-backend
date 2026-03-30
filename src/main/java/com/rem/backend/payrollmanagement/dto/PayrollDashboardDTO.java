package com.rem.backend.payrollmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollDashboardDTO {
    private long totalEmployees;
    private long activeEmployees;
    private long totalDepartments;
    private BigDecimal totalMonthlyPayroll;
    private long pendingLeaveRequests;
    private long presentToday;
    private long absentToday;
    private long payslipsGeneratedThisMonth;
}
