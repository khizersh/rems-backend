package com.rem.backend.repository;

import com.rem.backend.entity.employee.Allowance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllowanceRepository extends JpaRepository<Allowance, Long> {
    List<Allowance> findByEmployeeId(Long employeeId);
}