package com.rem.backend.payrollmanagement.repository;

import com.rem.backend.payrollmanagement.entity.SalaryAmendment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryAmendmentRepository extends JpaRepository<SalaryAmendment, Long> {
    List<SalaryAmendment> findByEmployeeIdAndSalaryMonthAndSalaryYear(Long employeeId, Integer month, Integer year);
    List<SalaryAmendment> findByEmployeeIdAndSalaryMonthAndSalaryYearAndStatus(Long employeeId, Integer month, Integer year, String status);
    List<SalaryAmendment> findByOrganizationIdAndSalaryMonthAndSalaryYear(Long organizationId, Integer month, Integer year);
    List<SalaryAmendment> findByEmployeeId(Long employeeId);
    List<SalaryAmendment> findByOrganizationId(Long organizationId);
}