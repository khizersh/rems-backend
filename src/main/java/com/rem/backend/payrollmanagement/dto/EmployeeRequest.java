package com.rem.backend.payrollmanagement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class EmployeeRequest {
    private Long organizationId;
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

    // Job info
    private Long departmentId;
    private String designation;
    private String employmentType;
    private String status;
    private LocalDate joiningDate;

    // Financial info
    private BigDecimal basicSalary;
    private String bankName;
    private String bankAccountNumber;
    private String bankBranchCode;

    // Allowances & Deductions
    private List<AllowanceRequest> allowances;
    private List<DeductionRequest> deductions;
}
