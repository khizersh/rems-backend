package com.rem.backend.payrollmanagement.repository;

import com.rem.backend.payrollmanagement.entity.SalarySlip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalarySlipRepository extends JpaRepository<SalarySlip, Long> {
    List<SalarySlip> findByEmployeeId(Long employeeId);
    Page<SalarySlip> findByEmployeeId(Long employeeId, Pageable pageable);
    List<SalarySlip> findByOrganizationIdAndSalaryMonthAndSalaryYear(Long organizationId, Integer month, Integer year);
    Optional<SalarySlip> findByEmployeeIdAndSalaryMonthAndSalaryYear(Long employeeId, Integer month, Integer year);
    Page<SalarySlip> findByOrganizationId(Long organizationId, Pageable pageable);
    long countByOrganizationIdAndSalaryMonthAndSalaryYear(Long organizationId, Integer month, Integer year);
}
