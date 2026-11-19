package com.rem.backend.dto.projectcost;

import com.rem.backend.projectmanagement.enums.ProjectCostCategory;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProjectCostRequest {
    private String title;
    private String description;
    private ProjectCostCategory category;
    private String referenceNumber;
    private LocalDate costDate;
    private String supportingDocumentPath;
}
