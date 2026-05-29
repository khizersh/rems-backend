package com.rem.backend.payrollmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryAmendmentResponse {
    private Long id;
    private Long organizationId;
    private Long employeeId;
    private String employeeName;
    private BigDecimal amount;
    private String amendmentType;
    private String description;
    private String status;
    private Integer salaryMonth;
    private Integer salaryYear;
    private LocalDateTime createdDate;
}

