package com.rem.backend.payrollmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private Integer payrollMonth;

    @Column(nullable = false)
    private Integer payrollYear;

    private String status; // DRAFT, PROCESSING, COMPLETED, CANCELLED

    private Integer totalEmployees;

    private BigDecimal totalBasicSalary;
    private BigDecimal totalAllowances;
    private BigDecimal totalDeductions;
    private BigDecimal totalAmendments;
    private BigDecimal totalNetSalary;

    private String processedBy;

    private LocalDateTime createdDate;
    private LocalDateTime processedDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        if (this.status == null) this.status = "DRAFT";
    }
}
