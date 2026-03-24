package com.rem.backend.payrollmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_slips")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SalarySlip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String departmentName;
    private String designation;

    private Integer salaryMonth;
    private Integer salaryYear;

    private BigDecimal basicSalary;
    private BigDecimal totalAllowances;
    private BigDecimal totalDeductions;
    private BigDecimal totalAmendments;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;

    private String status; // GENERATED, PAID, CANCELLED

    private LocalDateTime generatedDate;
    private LocalDateTime paidDate;

    @PrePersist
    protected void onCreate() {
        this.generatedDate = LocalDateTime.now();
        if (this.status == null) this.status = "GENERATED";
    }
}