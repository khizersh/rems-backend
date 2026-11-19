package com.rem.backend.dto.projectcost;

import com.rem.backend.projectmanagement.enums.ProjectCostCategory;
import com.rem.backend.projectmanagement.enums.ProjectCostPaymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateProjectCostRequest {
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
    private ProjectCostPaymentType paymentType;
    private Long organizationAccountId;
    private String paymentMethod;
    private String referenceNumber;
    private LocalDate costDate;
    private LocalDate chequeDate;
    private String chequeNumber;
    private String supportingDocumentPath;
}
