package com.rem.backend.analytics.reporting.admin.query;

import com.rem.backend.bookingmanagement.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Reporting aggregations over bookings. Booking value is not stored on the booking row;
 * it is derived from the CUSTOMER payment schedule joined on the unit (mirroring the
 * existing {@code BookingRepository.findMonthlyBookingStatsNativeByAmount} convention).
 */
@Repository
public interface AdminBookingReportRepository extends JpaRepository<Booking, Long> {

    @Query(value = "SELECT COUNT(*) FROM booking WHERE organization_id = :organizationId AND is_active = 1",
            nativeQuery = true)
    long countActiveBookings(@Param("organizationId") long organizationId);

    @Query(value = "SELECT COUNT(*) FROM booking WHERE organization_id = :organizationId "
            + "AND is_active = 1 AND booking_complete = 1", nativeQuery = true)
    long countCompletedBookings(@Param("organizationId") long organizationId);

    @Query(value = "SELECT COUNT(*) FROM booking WHERE organization_id = :organizationId "
            + "AND is_active = 1 AND created_date BETWEEN :startDate AND :endDate", nativeQuery = true)
    long countActiveBookingsInRange(@Param("organizationId") long organizationId,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM booking WHERE organization_id = :organizationId AND is_active = 0",
            nativeQuery = true)
    long countCancelledBookings(@Param("organizationId") long organizationId);

    /** Total contracted booking value for active bookings (sum of CUSTOMER schedule totals). */
    @Query(value = """
            SELECT COALESCE(SUM(ps.total_amount), 0)
            FROM booking b
            JOIN payment_schedule ps ON b.unit_id = ps.unit_id
            WHERE b.organization_id = :organizationId
              AND b.is_active = 1
              AND ps.payment_schedule_type = 'CUSTOMER'
            """, nativeQuery = true)
    Double totalBookingValue(@Param("organizationId") long organizationId);

    /** Monthly booking count for the org within a date range (for trend charts). */
    @Query(value = """
            SELECT YEAR(created_date) AS yr, MONTH(created_date) AS monthNo, COUNT(id) AS cnt
            FROM booking
            WHERE organization_id = :organizationId
              AND is_active = 1
              AND created_date BETWEEN :startDate AND :endDate
            GROUP BY YEAR(created_date), MONTH(created_date)
            ORDER BY yr, monthNo
            """, nativeQuery = true)
    List<Map<String, Object>> monthlyBookingCount(@Param("organizationId") long organizationId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
}
