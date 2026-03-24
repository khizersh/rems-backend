package com.rem.backend.payrollmanagement.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AllowanceRequest {
    private Long id; // null for new, set for update
    private String allowanceName;
    private BigDecimal amount;
    private Boolean isRecurring;
    private Boolean isActive;
}
