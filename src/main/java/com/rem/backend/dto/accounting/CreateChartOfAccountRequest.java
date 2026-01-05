package com.rem.backend.dto.accounting;

import lombok.Data;

@Data
public class CreateChartOfAccountRequest {

    private String code;
    private String name;
    private long accountGroupId;
    private Long projectId;
    private Long unitId;
    private Long customerId;
    private Long vendorId;
}
