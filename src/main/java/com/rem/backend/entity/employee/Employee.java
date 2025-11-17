package com.rem.backend.entity.employee;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "employees")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private Long departmentId;

    // Personal info
    private String fullName;
    private String email;
    private String phone;

    // Job info
    private String designation;




    private String joiningDate;   // keep String for now, change later

    // Financial info
    private BigDecimal basicSalary;
    private String bankAccountNumber;
}