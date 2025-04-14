package com.rem.backend.dto.project;

import lombok.Data;

@Data
public class ProjectPaginationRequest {
    private long organizationId;
    private long id;
    private int page = 0;
    private int size = 10;
    private String sortBy = "projectId";
    private String sortDir = "desc";
}