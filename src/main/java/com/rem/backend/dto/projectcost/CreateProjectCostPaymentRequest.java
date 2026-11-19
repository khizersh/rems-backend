package com.rem.backend.dto.projectcost;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateProjectCostPaymentRequest {
    private Long organizationId;
    private Long organizationAccountId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String referenceNumber;
    private String remarks;
}
