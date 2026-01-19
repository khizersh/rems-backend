package com.rem.backend.dto.accounting;

import lombok.Data;

@Data
public class CreateChartOfAccountRequest {

    private String code;
    private String name;
    private long accountGroupId;
}
