package com.rem.backend.payrollmanagement.repository;

import com.rem.backend.payrollmanagement.entity.Payroll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    Page<Payroll> findByOrganizationId(Long organizationId, Pageable pageable);
    Optional<Payroll> findByOrganizationIdAndPayrollMonthAndPayrollYear(Long organizationId, Integer month, Integer year);
    boolean existsByOrganizationIdAndPayrollMonthAndPayrollYear(Long organizationId, Integer month, Integer year);
}
