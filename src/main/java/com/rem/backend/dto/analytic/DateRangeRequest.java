package com.rem.backend.dto.analytic;


import com.rem.backend.enums.TransactionType;
import lombok.Data;

@Data
public class DateRangeRequest {

    private long organizationId;
    private String startDate;
    private String endDate;
    private TransactionType transactionType;
    private String filteredBy;
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdDate";
    private String sortDir = "asc";
}
