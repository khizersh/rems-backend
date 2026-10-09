package com.rem.backend.analytics.reporting.hr.query;

import com.rem.backend.analytics.reporting.hr.query.projection.LabeledCountProjection;
import com.rem.backend.analytics.reporting.hr.query.projection.MonthlyAttendanceProjection;
import com.rem.backend.payrollmanagement.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Reporting-only aggregate queries over {@code employee_attendance}. Isolated from
 * {@code AttendanceRepository} so reporting aggregations never touch the day-to-day
 * check-in/check-out write paths.
 */
@Repository
public interface HrAttendanceReportRepository extends JpaRepository<Attendance, Long> {

    /** Attendance rows grouped by status (PRESENT, ABSENT, LATE, HALF_DAY, ON_LEAVE) for a range. */
    @Query(value = """
            SELECT COALESCE(status, 'UNKNOWN') AS label, COUNT(*) AS count
            FROM employee_attendance
            WHERE organization_id = :organizationId
              AND attendance_date BETWEEN :startDate AND :endDate
            GROUP BY status
            """, nativeQuery = true)
    List<LabeledCountProjection> statusBreakdown(@Param("organizationId") long organizationId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /** Attendance status breakdown for a single day (used for a "today" snapshot). */
    @Query(value = """
            SELECT COALESCE(status, 'UNKNOWN') AS label, COUNT(*) AS count
            FROM employee_attendance
            WHERE organization_id = :organizationId AND attendance_date = :date
            GROUP BY status
            """, nativeQuery = true)
    List<LabeledCountProjection> statusBreakdownForDate(@Param("organizationId") long organizationId,
                                                        @Param("date") LocalDate date);

    /** Monthly present-vs-total attendance rows, used to derive an attendance-rate trend. */
    @Query(value = """
            SELECT YEAR(attendance_date)  AS yr,
                   MONTH(attendance_date) AS monthNo,
                   SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS presentCount,
                   COUNT(*) AS totalCount
            FROM employee_attendance
            WHERE organization_id = :organizationId
              AND attendance_date BETWEEN :startDate AND :endDate
            GROUP BY YEAR(attendance_date), MONTH(attendance_date)
            ORDER BY yr, monthNo
            """, nativeQuery = true)
    List<MonthlyAttendanceProjection> monthlyAttendanceRate(@Param("organizationId") long organizationId,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);
}
