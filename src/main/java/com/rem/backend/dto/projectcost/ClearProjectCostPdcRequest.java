package com.rem.backend.dto.projectcost;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ClearProjectCostPdcRequest {
    private Long organizationId;
    private Long organizationAccountId;
    private LocalDate paymentDate;
    private String referenceNumber;
    private String remarks;
}
