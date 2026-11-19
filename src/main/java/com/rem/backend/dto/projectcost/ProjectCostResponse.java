package com.rem.backend.dto.projectcost;

import com.rem.backend.projectmanagement.enums.ProjectCostCategory;
import com.rem.backend.projectmanagement.enums.ProjectCostPaymentType;
import com.rem.backend.projectmanagement.enums.ProjectCostStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectCostResponse {
    private Long id;
    private Long organizationId;
    private Long projectId;
    private String title;
    private String description;
    private ProjectCostCategory category;
    private Long expenseTypeId;
    private Long vendorId;
    private String payeeName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private ProjectCostPaymentType paymentType;
    private ProjectCostStatus status;
    private Long organizationAccountId;
    private String referenceNumber;
    private LocalDate costDate;
    private LocalDate chequeDate;
    private String chequeNumber;
    private String supportingDocumentPath;
    private LocalDateTime createdDate;
    private List<ProjectCostPaymentResponse> payments;
}
