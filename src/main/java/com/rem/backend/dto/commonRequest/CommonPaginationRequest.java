package com.rem.backend.dto.commonRequest;

import lombok.Data;

@Data
public class CommonPaginationRequest {
    private long id;
    private int page = 0;
    private int size = 10;
    private String sortBy = "";
    private String sortDir = "";
}