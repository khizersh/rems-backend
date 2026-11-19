package com.rem.backend.dto.projectcost;

import com.rem.backend.projectmanagement.enums.ProjectCostCategory;
import com.rem.backend.projectmanagement.enums.ProjectCostStatus;
import lombok.Data;

@Data
public class ProjectCostFilterRequest {
    private Long organizationId;
    private Long projectId;
    private ProjectCostCategory category;
    private ProjectCostStatus status;
    private Long vendorId;
    private int page = 0;
    private int size = 20;
    private String sortBy = "costDate";
    private String sortDir = "desc";
}
