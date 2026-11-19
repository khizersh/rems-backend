package com.rem.backend.dto.projectcost;

import lombok.Data;

@Data
public class CancelProjectCostRequest {
    private Long organizationId;
    private String reason;
}
