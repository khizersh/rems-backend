package com.rem.backend.payrollmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "employee_deductions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Deduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @JsonIgnoreProperties({"allowances", "deductions", "department", "hibernateLazyInitializer"})
    private Employee employee;

    @Column(name = "employee_id", insertable = false, updatable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String deductionName; // TAX, PROVIDENT_FUND, LOAN, INSURANCE, OTHER

    @Column(nullable = false)
    private BigDecimal amount;

    private Boolean isRecurring;

    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (this.isRecurring == null) this.isRecurring = true;
        if (this.isActive == null) this.isActive = true;
    }
}
