package com.rem.backend.payrollmanagement.repository;

import com.rem.backend.payrollmanagement.entity.Deduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeductionRepository extends JpaRepository<Deduction, Long> {
    List<Deduction> findByEmployeeId(Long employeeId);
    List<Deduction> findByEmployeeIdAndIsActiveTrue(Long employeeId);
    List<Deduction> findByOrganizationId(Long organizationId);
    void deleteByEmployeeId(Long employeeId);
}
