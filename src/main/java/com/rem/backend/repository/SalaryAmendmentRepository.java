package com.rem.backend.repository;

import com.rem.backend.entity.employee.SalaryAmendment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryAmendmentRepository extends JpaRepository<SalaryAmendment, Long> {
    List<SalaryAmendment> findByEmployeeIdAndSalaryMonthAndSalaryYear(Long employeeId, Integer month, Integer year);
}