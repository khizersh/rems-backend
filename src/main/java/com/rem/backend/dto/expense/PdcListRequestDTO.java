package com.rem.backend.dto.expense;

import lombok.Data;

@Data
public class PdcListRequestDTO {

    private long organizationId;
    private Long vendorAccountId;
    private Long projectId;

    /**
     * Filter type: dueToday | upcoming | overdue | all
     */
    private String filter = "all";

    private int page = 0;
    private int size = 10;
    private String sortBy = "chequeDate";
    private String sortDir = "asc";
}