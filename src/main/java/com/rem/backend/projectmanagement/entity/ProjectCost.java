package com.rem.backend.projectmanagement.entity;

import com.rem.backend.projectmanagement.enums.ProjectCostCategory;
import com.rem.backend.projectmanagement.enums.ProjectCostPaymentType;
import com.rem.backend.projectmanagement.enums.ProjectCostStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_cost", indexes = {
        @Index(name = "idx_project_cost_organization", columnList = "organization_id"),
        @Index(name = "idx_project_cost_project", columnList = "project_id"),
        @Index(name = "idx_project_cost_vendor", columnList = "vendor_id"),
        @Index(name = "idx_project_cost_category", columnList = "category"),
        @Index(name = "idx_project_cost_status", columnList = "status")
})
@Data
public class ProjectCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectCostCategory category;

    private Long expenseTypeId;
    private Long vendorId;
    private String payeeName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal pendingAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectCostPaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectCostStatus status;

    private Long organizationAccountId;
    private String referenceNumber;
    private LocalDate costDate;
    private LocalDate chequeDate;
    private String chequeNumber;
    private String supportingDocumentPath;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdDate == null) createdDate = now;
        if (updatedDate == null) updatedDate = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
