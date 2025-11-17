package com.rem.backend.entity.employee;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "salary_slips")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SalarySlip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    private Integer salaryMonth;
    private Integer salaryYear;

    private BigDecimal basicSalary;
    private BigDecimal totalAllowances;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
}