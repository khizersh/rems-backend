package com.rem.backend.dto.customer;

import lombok.Data;

@Data
public class CustomerPaginationRequest {
    private long id;
    private String filteredBy;
    private int page = 0;
    private int size = 10;
    private String sortBy = "floor";
    private String sortDir = "asc";
}