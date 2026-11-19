package com.rem.backend.dto.projectcost;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProjectCostPaymentResponse {
    private Long id;
    private BigDecimal amount;
    private Long organizationAccountId;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String referenceNumber;
    private String remarks;
}
