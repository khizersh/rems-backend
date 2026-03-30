package com.rem.backend.payrollmanagement.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepartmentRequest {
    private Long organizationId;
    private String name;
    private String description;
    private String departmentHead;
    private BigDecimal budgetAllocated;
    private Boolean isActive;
}
