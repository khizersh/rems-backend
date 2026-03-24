package com.rem.backend.payrollmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_amendments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SalaryAmendment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;
    private Long employeeId;
    private BigDecimal amount;

    // Type: ADDITION / DEDUCTION
    private String amendmentType;

    private String description;

    private String status; // PENDING, APPROVED, REJECTED

    // For month-wise salary processing
    private Integer salaryMonth;
    private Integer salaryYear;

    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        if (this.status == null) this.status = "APPROVED";
    }
}