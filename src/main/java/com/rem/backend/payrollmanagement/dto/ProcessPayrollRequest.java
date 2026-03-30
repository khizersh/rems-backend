package com.rem.backend.payrollmanagement.dto;

import lombok.Data;

@Data
public class ProcessPayrollRequest {
    private Long organizationId;
    private Integer payrollMonth;
    private Integer payrollYear;
    private String processedBy;
}
