package com.rem.backend.payrollmanagement.service;

import com.rem.backend.payrollmanagement.dto.SalaryAmendmentRequest;
import com.rem.backend.payrollmanagement.dto.SalaryAmendmentResponse;
import com.rem.backend.payrollmanagement.entity.Employee;
import com.rem.backend.payrollmanagement.repository.EmployeeRepository;
import com.rem.backend.payrollmanagement.entity.SalaryAmendment;
import com.rem.backend.payrollmanagement.repository.SalaryAmendmentRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SalaryAmendmentService {

    private final SalaryAmendmentRepository salaryAmendmentRepository;
    private final EmployeeRepository employeeRepository;

    public Map<String, Object> createAmendment(SalaryAmendmentRequest request) {
        try {
            SalaryAmendment amendment = SalaryAmendment.builder()
                    .organizationId(request.getOrganizationId())
                    .employeeId(request.getEmployeeId())
                    .amount(request.getAmount())
                    .amendmentType(request.getAmendmentType())
                    .description(request.getDescription())
                    .status(request.getStatus() != null ? request.getStatus() : "APPROVED")
                    .salaryMonth(request.getSalaryMonth())
                    .salaryYear(request.getSalaryYear())
                    .build();

            SalaryAmendment saved = salaryAmendmentRepository.save(amendment);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> updateAmendment(Long id, SalaryAmendmentRequest request) {
        try {
            Optional<SalaryAmendment> optional = salaryAmendmentRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Amendment not found");
            }

            SalaryAmendment amendment = optional.get();
            if (request.getAmount() != null) amendment.setAmount(request.getAmount());
            if (request.getAmendmentType() != null) amendment.setAmendmentType(request.getAmendmentType());
            if (request.getDescription() != null) amendment.setDescription(request.getDescription());
            if (request.getStatus() != null) amendment.setStatus(request.getStatus());
            if (request.getSalaryMonth() != null) amendment.setSalaryMonth(request.getSalaryMonth());
            if (request.getSalaryYear() != null) amendment.setSalaryYear(request.getSalaryYear());

            SalaryAmendment saved = salaryAmendmentRepository.save(amendment);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAmendmentsByEmployee(Long employeeId) {
        try {
            List<SalaryAmendment> amendments = salaryAmendmentRepository.findByEmployeeId(employeeId);
            java.util.List<SalaryAmendmentResponse> responses = new java.util.ArrayList<>();
            for (SalaryAmendment a : amendments) {
                String employeeName = "";
                try {
                    Employee emp = employeeRepository.findById(a.getEmployeeId()).orElse(null);
                    if (emp != null) employeeName = emp.getFullName();
                } catch (Exception ignored) {}

                SalaryAmendmentResponse resp = SalaryAmendmentResponse.builder()
                        .id(a.getId())
                        .organizationId(a.getOrganizationId())
                        .employeeId(a.getEmployeeId())
                        .employeeName(employeeName)
                        .amount(a.getAmount())
                        .amendmentType(a.getAmendmentType())
                        .description(a.getDescription())
                        .status(a.getStatus())
                        .salaryMonth(a.getSalaryMonth())
                        .salaryYear(a.getSalaryYear())
                        .createdDate(a.getCreatedDate())
                        .build();
                responses.add(resp);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, responses);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAmendmentsByOrganizationAndMonth(Long organizationId, Integer month, Integer year) {
        try {
            List<SalaryAmendment> amendments = salaryAmendmentRepository.findByOrganizationIdAndSalaryMonthAndSalaryYear(organizationId, month, year);
            java.util.List<SalaryAmendmentResponse> responses = new java.util.ArrayList<>();
            for (SalaryAmendment a : amendments) {
                String employeeName = "";
                try {
                    Employee emp = employeeRepository.findById(a.getEmployeeId()).orElse(null);
                    if (emp != null) employeeName = emp.getFullName();
                } catch (Exception ignored) {}

                SalaryAmendmentResponse resp = SalaryAmendmentResponse.builder()
                        .id(a.getId())
                        .organizationId(a.getOrganizationId())
                        .employeeId(a.getEmployeeId())
                        .employeeName(employeeName)
                        .amount(a.getAmount())
                        .amendmentType(a.getAmendmentType())
                        .description(a.getDescription())
                        .status(a.getStatus())
                        .salaryMonth(a.getSalaryMonth())
                        .salaryYear(a.getSalaryYear())
                        .createdDate(a.getCreatedDate())
                        .build();
                responses.add(resp);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, responses);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> deleteAmendment(Long id) {
        try {
            Optional<SalaryAmendment> optional = salaryAmendmentRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Amendment not found");
            }
            salaryAmendmentRepository.deleteById(id);
            return ResponseMapper.buildResponse(Responses.SUCCESS, "Amendment deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
