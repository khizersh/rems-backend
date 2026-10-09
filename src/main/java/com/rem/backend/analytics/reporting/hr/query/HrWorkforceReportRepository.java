package com.rem.backend.analytics.reporting.hr.query;

import com.rem.backend.analytics.reporting.hr.query.projection.LabeledCountProjection;
import com.rem.backend.analytics.reporting.hr.query.projection.MonthlyCountProjection;
import com.rem.backend.payrollmanagement.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Reporting-only aggregate queries over {@code employees}. Kept isolated from
 * {@code EmployeeRepository} so HR reporting aggregations never interfere with the
 * transactional employee CRUD/search paths.
 */
@Repository
public interface HrWorkforceReportRepository extends JpaRepository<Employee, Long> {

    /** Headcount grouped by employee status (ACTIVE, INACTIVE, TERMINATED, ON_LEAVE). */
    @Query(value = """
            SELECT COALESCE(status, 'UNKNOWN') AS label, COUNT(*) AS count
            FROM employees
            WHERE organization_id = :organizationId
            GROUP BY status
            """, nativeQuery = true)
    List<LabeledCountProjection> headcountByStatus(@Param("organizationId") long organizationId);

    /** Headcount grouped by department name (employees without a department are "Unassigned"). */
    @Query(value = """
            SELECT COALESCE(d.name, 'Unassigned') AS label, COUNT(*) AS count
            FROM employees e
            LEFT JOIN department d ON d.id = e.department_id
            WHERE e.organization_id = :organizationId
            GROUP BY d.name
            ORDER BY count DESC
            """, nativeQuery = true)
    List<LabeledCountProjection> headcountByDepartment(@Param("organizationId") long organizationId);

    /** Headcount grouped by employment type (FULL_TIME, PART_TIME, CONTRACT, INTERN). */
    @Query(value = """
            SELECT COALESCE(employment_type, 'UNSPECIFIED') AS label, COUNT(*) AS count
            FROM employees
            WHERE organization_id = :organizationId
            GROUP BY employment_type
            """, nativeQuery = true)
    List<LabeledCountProjection> headcountByEmploymentType(@Param("organizationId") long organizationId);

    /** Headcount grouped by gender. */
    @Query(value = """
            SELECT COALESCE(gender, 'UNSPECIFIED') AS label, COUNT(*) AS count
            FROM employees
            WHERE organization_id = :organizationId
            GROUP BY gender
            """, nativeQuery = true)
    List<LabeledCountProjection> headcountByGender(@Param("organizationId") long organizationId);

    /** Monthly new hires (by {@code joiningDate}) within the given date range. */
    @Query(value = """
            SELECT YEAR(joining_date) AS yr, MONTH(joining_date) AS monthNo, COUNT(*) AS count
            FROM employees
            WHERE organization_id = :organizationId
              AND joining_date BETWEEN :startDate AND :endDate
            GROUP BY YEAR(joining_date), MONTH(joining_date)
            ORDER BY yr, monthNo
            """, nativeQuery = true)
    List<MonthlyCountProjection> monthlyHires(@Param("organizationId") long organizationId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /** Monthly separations (by {@code terminationDate}) within the given date range. */
    @Query(value = """
            SELECT YEAR(termination_date) AS yr, MONTH(termination_date) AS monthNo, COUNT(*) AS count
            FROM employees
            WHERE organization_id = :organizationId
              AND termination_date BETWEEN :startDate AND :endDate
            GROUP BY YEAR(termination_date), MONTH(termination_date)
            ORDER BY yr, monthNo
            """, nativeQuery = true)
    List<MonthlyCountProjection> monthlyTerminations(@Param("organizationId") long organizationId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    /** Average basic salary across active employees (used for a quick compensation KPI). */
    @Query(value = """
            SELECT AVG(basic_salary)
            FROM employees
            WHERE organization_id = :organizationId AND status = 'ACTIVE'
            """, nativeQuery = true)
    Double averageActiveBasicSalary(@Param("organizationId") long organizationId);
}
