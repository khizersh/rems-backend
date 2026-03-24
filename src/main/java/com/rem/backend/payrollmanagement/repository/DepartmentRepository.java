package com.rem.backend.payrollmanagement.repository;

import com.rem.backend.payrollmanagement.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Page<Department> findByOrganizationId(Long organizationId, Pageable pageable);
    List<Department> findByOrganizationId(Long organizationId);
    List<Department> findByOrganizationIdAndIsActiveTrue(Long organizationId);
    Page<Department> findByOrganizationIdAndNameContainingIgnoreCase(Long organizationId, String name, Pageable pageable);
    long countByOrganizationId(Long organizationId);
}
