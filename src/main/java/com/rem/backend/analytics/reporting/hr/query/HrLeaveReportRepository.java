package com.rem.backend.analytics.reporting.hr.query;

import com.rem.backend.analytics.reporting.hr.query.projection.LabeledAmountProjection;
import com.rem.backend.analytics.reporting.hr.query.projection.LabeledCountProjection;
import com.rem.backend.analytics.reporting.hr.query.projection.MonthlyAmountSumProjection;
import com.rem.backend.payrollmanagement.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reporting-only aggregate queries over {@code leave_requests}. Isolated from
 * {@code LeaveRequestRepository} so reporting never interferes with the leave
 * request/approval workflow.
 */
@Repository
public interface HrLeaveReportRepository extends JpaRepository<LeaveRequest, Long> {

    /** Leave requests grouped by status (PENDING, APPROVED, REJECTED, CANCELLED). */
    @Query(value = """
            SELECT COALESCE(status, 'UNKNOWN') AS label, COUNT(*) AS count
            FROM leave_requests
            WHERE organization_id = :organizationId
              AND start_date <= :endDate AND end_date >= :startDate
            GROUP BY status
            """, nativeQuery = true)
    List<LabeledCountProjection> statusBreakdown(@Param("organizationId") long organizationId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /** Approved leave days grouped by leave type (ANNUAL, SICK, CASUAL, ...) within a range. */
    @Query(value = """
            SELECT COALESCE(leave_type, 'UNKNOWN') AS label, COALESCE(SUM(total_days), 0) AS amount
            FROM leave_requests
            WHERE organization_id = :organizationId
              AND status = 'APPROVED'
              AND start_date <= :endDate AND end_date >= :startDate
            GROUP BY leave_type
            """, nativeQuery = true)
    List<LabeledAmountProjection> leaveDaysByType(@Param("organizationId") long organizationId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    /** Monthly approved leave days (by {@code startDate}) within a range, for trend charts. */
    @Query(value = """
            SELECT YEAR(start_date)  AS yr,
                   MONTH(start_date) AS monthNo,
                   COALESCE(SUM(total_days), 0) AS amount
            FROM leave_requests
            WHERE organization_id = :organizationId
              AND status = 'APPROVED'
              AND start_date BETWEEN :startDate AND :endDate
            GROUP BY YEAR(start_date), MONTH(start_date)
            ORDER BY yr, monthNo
            """, nativeQuery = true)
    List<MonthlyAmountSumProjection> monthlyApprovedLeaveDays(@Param("organizationId") long organizationId,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);

    /**
     * Pending leave requests joined with employee/department names for a drill-down table.
     * Capped by {@code limit} (applied via Pageable-free native LIMIT for simplicity).
     */
    @Query(value = """
            SELECT lr.id AS id,
                   e.full_name AS employeeName,
                   COALESCE(d.name, 'Unassigned') AS departmentName,
                   lr.leave_type AS leaveType,
                   lr.start_date AS startDate,
                   lr.end_date AS endDate,
                   lr.total_days AS totalDays,
                   lr.reason AS reason
            FROM leave_requests lr
            JOIN employees e ON e.id = lr.employee_id
            LEFT JOIN department d ON d.id = e.department_id
            WHERE lr.organization_id = :organizationId AND lr.status = 'PENDING'
            ORDER BY lr.start_date ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Map<String, Object>> pendingLeaveRequests(@Param("organizationId") long organizationId,
                                                   @Param("limit") int limit);

    /** Total count of pending leave requests for the org (used for an alert threshold). */
    @Query(value = "SELECT COUNT(*) FROM leave_requests WHERE organization_id = :organizationId AND status = 'PENDING'",
            nativeQuery = true)
    long countPending(@Param("organizationId") long organizationId);
}
