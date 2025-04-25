package com.rem.backend.dto.customerAccount;

import lombok.Data;

@Data
public class CustomerAccountPaginationRequest {
    private long id;
    private int page = 0;
    private int size = 10;
    private String sortBy = "";
    private String sortDir = "";
}