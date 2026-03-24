package com.rem.backend.payrollmanagement.service;

import com.rem.backend.payrollmanagement.dto.DepartmentRequest;
import com.rem.backend.payrollmanagement.entity.Department;
import com.rem.backend.payrollmanagement.repository.DepartmentRepository;
import com.rem.backend.payrollmanagement.repository.EmployeeRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public Map<String, Object> createDepartment(DepartmentRequest request) {
        try {
            Department department = Department.builder()
                    .organizationId(request.getOrganizationId())
                    .name(request.getName())
                    .description(request.getDescription())
                    .departmentHead(request.getDepartmentHead())
                    .budgetAllocated(request.getBudgetAllocated())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .build();

            Department saved = departmentRepository.save(department);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> updateDepartment(Long id, DepartmentRequest request) {
        try {
            Optional<Department> optional = departmentRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Department not found");
            }

            Department department = optional.get();
            if (request.getName() != null) department.setName(request.getName());
            if (request.getDescription() != null) department.setDescription(request.getDescription());
            if (request.getDepartmentHead() != null) department.setDepartmentHead(request.getDepartmentHead());
            if (request.getBudgetAllocated() != null) department.setBudgetAllocated(request.getBudgetAllocated());
            if (request.getIsActive() != null) department.setIsActive(request.getIsActive());

            Department saved = departmentRepository.save(department);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getDepartmentById(Long id) {
        try {
            Optional<Department> optional = departmentRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Department not found");
            }

            Department department = optional.get();
            long employeeCount = employeeRepository.countByDepartmentId(department.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("department", department);
            result.put("employeeCount", employeeCount);

            return ResponseMapper.buildResponse(Responses.SUCCESS, result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getDepartmentsByOrganization(Long organizationId, Pageable pageable) {
        try {
            Page<Department> departments = departmentRepository.findByOrganizationId(organizationId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, departments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getActiveDepartments(Long organizationId) {
        try {
            List<Department> departments = departmentRepository.findByOrganizationIdAndIsActiveTrue(organizationId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, departments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> searchDepartments(Long organizationId, String name, Pageable pageable) {
        try {
            Page<Department> departments = departmentRepository.findByOrganizationIdAndNameContainingIgnoreCase(organizationId, name, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, departments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> deleteDepartment(Long id) {
        try {
            Optional<Department> optional = departmentRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Department not found");
            }

            long employeeCount = employeeRepository.countByDepartmentId(id);
            if (employeeCount > 0) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER,
                        "Cannot delete department with " + employeeCount + " active employees. Reassign employees first.");
            }

            departmentRepository.deleteById(id);
            return ResponseMapper.buildResponse(Responses.SUCCESS, "Department deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
