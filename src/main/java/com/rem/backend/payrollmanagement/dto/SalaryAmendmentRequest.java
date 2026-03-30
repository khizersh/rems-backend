package com.rem.backend.payrollmanagement.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryAmendmentRequest {
    private Long organizationId;
    private Long employeeId;
    private BigDecimal amount;
    private String amendmentType; // ADDITION / DEDUCTION
    private String description;
    private String status;
    private Integer salaryMonth;
    private Integer salaryYear;
}
