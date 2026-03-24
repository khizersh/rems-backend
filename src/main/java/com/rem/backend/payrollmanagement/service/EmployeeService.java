package com.rem.backend.payrollmanagement.service;

import com.rem.backend.payrollmanagement.dto.AllowanceRequest;
import com.rem.backend.payrollmanagement.dto.DeductionRequest;
import com.rem.backend.payrollmanagement.dto.EmployeeRequest;
import com.rem.backend.payrollmanagement.entity.*;
import com.rem.backend.payrollmanagement.repository.*;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AllowanceRepository allowanceRepository;
    private final DeductionRepository deductionRepository;

    @Transactional
    public Map<String, Object> createEmployee(EmployeeRequest request) {
        try {
            // Build employee
            Employee employee = Employee.builder()
                    .organizationId(request.getOrganizationId())
                    .employeeCode(request.getEmployeeCode())
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .cnic(request.getCnic())
                    .address(request.getAddress())
                    .city(request.getCity())
                    .gender(request.getGender())
                    .dateOfBirth(request.getDateOfBirth())
                    .designation(request.getDesignation())
                    .employmentType(request.getEmploymentType() != null ? request.getEmploymentType() : "FULL_TIME")
                    .status("ACTIVE")
                    .joiningDate(request.getJoiningDate())
                    .basicSalary(request.getBasicSalary())
                    .bankName(request.getBankName())
                    .bankAccountNumber(request.getBankAccountNumber())
                    .bankBranchCode(request.getBankBranchCode())
                    .build();

            // Set department relationship
            if (request.getDepartmentId() != null) {
                Optional<Department> dept = departmentRepository.findById(request.getDepartmentId());
                dept.ifPresent(employee::setDepartment);
            }

            Employee saved = employeeRepository.save(employee);

            // Save allowances
            if (request.getAllowances() != null) {
                for (AllowanceRequest ar : request.getAllowances()) {
                    Allowance allowance = Allowance.builder()
                            .organizationId(request.getOrganizationId())
                            .employee(saved)
                            .allowanceName(ar.getAllowanceName())
                            .amount(ar.getAmount())
                            .isRecurring(ar.getIsRecurring())
                            .isActive(ar.getIsActive())
                            .build();
                    allowanceRepository.save(allowance);
                }
            }

            // Save deductions
            if (request.getDeductions() != null) {
                for (DeductionRequest dr : request.getDeductions()) {
                    Deduction deduction = Deduction.builder()
                            .organizationId(request.getOrganizationId())
                            .employee(saved)
                            .deductionName(dr.getDeductionName())
                            .amount(dr.getAmount())
                            .isRecurring(dr.getIsRecurring())
                            .isActive(dr.getIsActive())
                            .build();
                    deductionRepository.save(deduction);
                }
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> updateEmployee(Long id, EmployeeRequest request) {
        try {
            Optional<Employee> optional = employeeRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Employee not found");
            }

            Employee employee = optional.get();

            if (request.getEmployeeCode() != null) employee.setEmployeeCode(request.getEmployeeCode());
            if (request.getFullName() != null) employee.setFullName(request.getFullName());
            if (request.getEmail() != null) employee.setEmail(request.getEmail());
            if (request.getPhone() != null) employee.setPhone(request.getPhone());
            if (request.getCnic() != null) employee.setCnic(request.getCnic());
            if (request.getAddress() != null) employee.setAddress(request.getAddress());
            if (request.getCity() != null) employee.setCity(request.getCity());
            if (request.getGender() != null) employee.setGender(request.getGender());
            if (request.getDateOfBirth() != null) employee.setDateOfBirth(request.getDateOfBirth());
            if (request.getDesignation() != null) employee.setDesignation(request.getDesignation());
            if (request.getEmploymentType() != null) employee.setEmploymentType(request.getEmploymentType());
            if (request.getStatus() != null) employee.setStatus(request.getStatus());
            if (request.getJoiningDate() != null) employee.setJoiningDate(request.getJoiningDate());
            if (request.getBasicSalary() != null) employee.setBasicSalary(request.getBasicSalary());
            if (request.getBankName() != null) employee.setBankName(request.getBankName());
            if (request.getBankAccountNumber() != null) employee.setBankAccountNumber(request.getBankAccountNumber());
            if (request.getBankBranchCode() != null) employee.setBankBranchCode(request.getBankBranchCode());

            if (request.getDepartmentId() != null) {
                Optional<Department> dept = departmentRepository.findById(request.getDepartmentId());
                dept.ifPresent(employee::setDepartment);
            }

            // Update allowances if provided
            if (request.getAllowances() != null) {
                // Remove old allowances and add new ones
                employee.getAllowances().clear();
                for (AllowanceRequest ar : request.getAllowances()) {
                    Allowance allowance = Allowance.builder()
                            .organizationId(employee.getOrganizationId())
                            .employee(employee)
                            .allowanceName(ar.getAllowanceName())
                            .amount(ar.getAmount())
                            .isRecurring(ar.getIsRecurring())
                            .isActive(ar.getIsActive())
                            .build();
                    employee.getAllowances().add(allowance);
                }
            }

            // Update deductions if provided
            if (request.getDeductions() != null) {
                employee.getDeductions().clear();
                for (DeductionRequest dr : request.getDeductions()) {
                    Deduction deduction = Deduction.builder()
                            .organizationId(employee.getOrganizationId())
                            .employee(employee)
                            .deductionName(dr.getDeductionName())
                            .amount(dr.getAmount())
                            .isRecurring(dr.getIsRecurring())
                            .isActive(dr.getIsActive())
                            .build();
                    employee.getDeductions().add(deduction);
                }
            }

            Employee saved = employeeRepository.save(employee);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getEmployeeById(Long id) {
        try {
            Optional<Employee> optional = employeeRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Employee not found");
            }
            return ResponseMapper.buildResponse(Responses.SUCCESS, optional.get());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getEmployeesByOrganization(Long organizationId, Pageable pageable) {
        try {
            Page<Employee> employees = employeeRepository.findByOrganizationId(organizationId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, employees);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getEmployeesByDepartment(Long organizationId, Long departmentId, Pageable pageable) {
        try {
            Page<Employee> employees = employeeRepository.findByOrganizationIdAndDepartmentId(organizationId, departmentId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, employees);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> searchEmployees(Long organizationId, String keyword, Pageable pageable) {
        try {
            Page<Employee> employees = employeeRepository.searchEmployees(organizationId, keyword, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, employees);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> terminateEmployee(Long id) {
        try {
            Optional<Employee> optional = employeeRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Employee not found");
            }

            Employee employee = optional.get();
            employee.setStatus("TERMINATED");
            employee.setTerminationDate(java.time.LocalDate.now());
            employeeRepository.save(employee);
            return ResponseMapper.buildResponse(Responses.SUCCESS, "Employee terminated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> deleteEmployee(Long id) {
        try {
            Optional<Employee> optional = employeeRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Employee not found");
            }
            employeeRepository.deleteById(id);
            return ResponseMapper.buildResponse(Responses.SUCCESS, "Employee deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
