package com.rem.backend.payrollmanagement.repository;

import com.rem.backend.payrollmanagement.entity.Allowance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllowanceRepository extends JpaRepository<Allowance, Long> {
    List<Allowance> findByEmployeeId(Long employeeId);
    List<Allowance> findByEmployeeIdAndIsActiveTrue(Long employeeId);
    List<Allowance> findByOrganizationId(Long organizationId);
    void deleteByEmployeeId(Long employeeId);
}