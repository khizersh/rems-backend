package com.rem.backend.payrollmanagement.repository;

import com.rem.backend.payrollmanagement.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Page<Employee> findByOrganizationId(Long organizationId, Pageable pageable);
    List<Employee> findByOrganizationIdAndStatus(Long organizationId, String status);
    Page<Employee> findByOrganizationIdAndDepartmentId(Long organizationId, Long departmentId, Pageable pageable);
    List<Employee> findByDepartmentIdAndStatus(Long departmentId, String status);
    List<Employee> findByOrganizationId(Long organizationId);

    Page<Employee> findByOrganizationIdAndFullNameContainingIgnoreCase(Long organizationId, String fullName, Pageable pageable);

    Optional<Employee> findByEmployeeCode(String employeeCode);

    long countByOrganizationId(Long organizationId);
    long countByOrganizationIdAndStatus(Long organizationId, String status);
    long countByDepartmentId(Long departmentId);

    @Query("SELECT e FROM Employee e WHERE e.organizationId = :orgId AND " +
           "(LOWER(e.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.designation) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Employee> searchEmployees(@Param("orgId") Long organizationId, @Param("keyword") String keyword, Pageable pageable);
}
