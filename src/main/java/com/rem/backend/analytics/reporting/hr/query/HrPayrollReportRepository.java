package com.rem.backend.analytics.reporting.hr.query;

import com.rem.backend.analytics.reporting.hr.query.projection.LabeledAmountProjection;
import com.rem.backend.analytics.reporting.hr.query.projection.LabeledCountProjection;
import com.rem.backend.payrollmanagement.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Reporting-only aggregate queries over payroll, salary slip, allowance and deduction tables.
 * Isolated from the transactional payroll repositories so reporting aggregations never
 * interfere with payroll processing.
 */
@Repository
public interface HrPayrollReportRepository extends JpaRepository<Payroll, Long> {

    /** All payroll runs for the org ordered chronologically (bounded in practice: one per month). */
    @Query("SELECT p FROM Payroll p WHERE p.organizationId = :organizationId ORDER BY p.payrollYear, p.payrollMonth")
    List<Payroll> findAllByOrganizationIdOrdered(@Param("organizationId") long organizationId);

    /** Salary slip counts grouped by status (GENERATED, PAID, CANCELLED) for a given month/year. */
    @Query(value = """
            SELECT COALESCE(status, 'UNKNOWN') AS label, COUNT(*) AS count
            FROM salary_slips
            WHERE organization_id = :organizationId AND salary_month = :month AND salary_year = :year
            GROUP BY status
            """, nativeQuery = true)
    List<LabeledCountProjection> salarySlipStatusBreakdown(@Param("organizationId") long organizationId,
                                                           @Param("month") int month,
                                                           @Param("year") int year);

    /** Net salary total grouped by department for a given month/year (denormalized on the slip). */
    @Query(value = """
            SELECT COALESCE(department_name, 'Unassigned') AS label, COALESCE(SUM(net_salary), 0) AS amount
            FROM salary_slips
            WHERE organization_id = :organizationId AND salary_month = :month AND salary_year = :year
            GROUP BY department_name
            ORDER BY amount DESC
            """, nativeQuery = true)
    List<LabeledAmountProjection> netSalaryByDepartment(@Param("organizationId") long organizationId,
                                                        @Param("month") int month,
                                                        @Param("year") int year);

    /** Active allowance totals grouped by allowance name, org-wide (current recurring setup). */
    @Query(value = """
            SELECT COALESCE(allowance_name, 'Other') AS label, COALESCE(SUM(amount), 0) AS amount
            FROM employee_allowances
            WHERE organization_id = :organizationId AND is_active = 1
            GROUP BY allowance_name
            ORDER BY amount DESC
            """, nativeQuery = true)
    List<LabeledAmountProjection> allowanceTotalsByName(@Param("organizationId") long organizationId);

    /** Active deduction totals grouped by deduction name, org-wide (current recurring setup). */
    @Query(value = """
            SELECT COALESCE(deduction_name, 'Other') AS label, COALESCE(SUM(amount), 0) AS amount
            FROM employee_deductions
            WHERE organization_id = :organizationId AND is_active = 1
            GROUP BY deduction_name
            ORDER BY amount DESC
            """, nativeQuery = true)
    List<LabeledAmountProjection> deductionTotalsByName(@Param("organizationId") long organizationId);

    /** Highest-paid active employees for the given month/year, capped by {@code limit}. */
    @Query(value = """
            SELECT employee_name AS employeeName, department_name AS departmentName,
                   designation AS designation, net_salary AS netSalary
            FROM salary_slips
            WHERE organization_id = :organizationId AND salary_month = :month AND salary_year = :year
            ORDER BY net_salary DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<java.util.Map<String, Object>> topEarners(@Param("organizationId") long organizationId,
                                                    @Param("month") int month,
                                                    @Param("year") int year,
                                                    @Param("limit") int limit);
}
