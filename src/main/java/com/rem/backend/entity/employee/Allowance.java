package com.rem.backend.entity.employee;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "employee_allowances")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Allowance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String allowanceName;

    @Column(nullable = false)
    private BigDecimal amount;


}