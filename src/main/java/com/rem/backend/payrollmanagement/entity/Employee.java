package com.rem.backend.payrollmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizationId;

    @Column(unique = true)
    private String employeeCode;

    // Personal info
    private String fullName;
    private String email;
    private String phone;
    private String cnic;
    private String address;
    private String city;
    private String gender;
    private LocalDate dateOfBirth;
    private String profileImageUrl;

    // Job info
    private String designation;
    private String employmentType; // FULL_TIME, PART_TIME, CONTRACT, INTERN

    private String status; // ACTIVE, INACTIVE, TERMINATED, ON_LEAVE

    private LocalDate joiningDate;
    private LocalDate terminationDate;

    // Department relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonIgnoreProperties({"employees", "hibernateLazyInitializer"})
    private Department department;

    @Column(name = "department_id", insertable = false, updatable = false)
    private Long departmentId;

    // Financial info
    private BigDecimal basicSalary;
    private String bankName;
    private String bankAccountNumber;
    private String bankBranchCode;

    // Allowances & Deductions
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("employee")
    @Builder.Default
    private List<Allowance> allowances = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("employee")
    @Builder.Default
    private List<Deduction> deductions = new ArrayList<>();

    // Timestamps
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}