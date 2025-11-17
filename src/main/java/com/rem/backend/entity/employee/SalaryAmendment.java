package com.rem.backend.entity.employee;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "salary_amendments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SalaryAmendment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;
    private BigDecimal amount;
    
    // Type: ADDITION / DEDUCTION
    private String amendmentType;

    // For month-wise salary processing
    private Integer salaryMonth; 
    private Integer salaryYear; 
}