package com.rem.backend.dto.unit;

import lombok.Data;

@Data
public class UnitPaginationRequest {
    private long floorId;
    private long id;
    private int page = 0;
    private int size = 10;
    private String sortBy = "";
    private String sortDir = "";
}