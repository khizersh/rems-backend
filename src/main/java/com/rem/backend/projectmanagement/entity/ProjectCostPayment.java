package com.rem.backend.projectmanagement.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_cost_payment", indexes = {
        @Index(name = "idx_project_cost_payment_cost", columnList = "project_cost_id"),
        @Index(name = "idx_project_cost_payment_account", columnList = "organization_account_id")
})
@Data
public class ProjectCostPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectCostId;

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private Long projectId;

    private Long vendorId;

    @Column(nullable = false)
    private Long organizationAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    private String paymentMethod;
    private String referenceNumber;
    private String remarks;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @Column(nullable = false)
    private boolean reversed = false;

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
