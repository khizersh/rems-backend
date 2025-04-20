package com.rem.backend.dto.floor;

import lombok.Data;

@Data
public class FloorPaginationRequest {
    private long projectId;
    private long id;
    private int page = 0;
    private int size = 10;
    private String sortBy = "floor";
    private String sortDir = "asc";
}