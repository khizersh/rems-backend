package com.rem.backend.dto.commonRequest;

import lombok.Data;

@Data
public class FilterPaginationRequest {
    private long id;
    private long id2;
    private String filteredBy;
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdDate";
    private String sortDir = "asc";
}