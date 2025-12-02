package com.rem.backend.repository;

import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByUnit_Id(Long unitId);

    Page<Booking> findByOrganizationIdAndIsActiveTrue(Long organizationId, Pageable pageable);
    Page<Booking> findByProjectIdAndIsActiveTrue(Long projectId, Pageable pageable);
    Page<Booking> findByFloorIdAndIsActiveTrue(Long projectId, Pageable pageable);

    Page<Booking> findByUnitIdAndIsActiveTrue(Long unitId, Pageable pageable);
    List<Booking> findByCustomerIdAndIsActiveTrue(long customerId);

    @Query("SELECT b.customer FROM Booking b WHERE b.projectId = :projectId AND b.isActive = true")
    Page<Customer> findCustomersByProjectId(@Param("projectId") Long projectId, Pageable pageable);

    // Customers by Unit
    @Query("SELECT b.customer FROM Booking b WHERE b.unit.id = :unitId AND b.isActive = true")
    Page<Customer> findCustomersByUnitId(@Param("unitId") Long unitId, Pageable pageable);

    // Customers by Floor
    @Query("SELECT b.customer FROM Booking b WHERE b.floorId = :floorId AND b.isActive = true")
    Page<Customer> findCustomersByFloorId(@Param("floorId") Long floorId, Pageable pageable);


    @Query(
            value = "SELECT MONTH(created_date) AS month, YEAR(created_date) AS year, COUNT(id) AS count " +
                    "FROM booking " +
                    "WHERE YEAR(created_date) IN (:currentYear, :lastYear) " +
                    "AND organization_id = :organizationId  AND isActive = true " +
                    "GROUP BY YEAR(created_date), MONTH(created_date) " +
                    "ORDER BY year, month",
            nativeQuery = true
    )
    List<Map<String , Object>> findMonthlyBookingStatsNativeByCount(@Param("currentYear") int currentYear,
                                                                    @Param("lastYear") int lastYear,
                                                                    @Param("organizationId") long organizationId);



    @Query(
            value = """
        SELECT 
            MONTH(b.created_date) AS month,
            YEAR(b.created_date) AS year,
            SUM(ps.total_amount) AS amount 
        FROM booking b
        JOIN payment_schedule ps ON b.unit_id = ps.unit_id
        WHERE YEAR(b.created_date) IN (:currentYear, :lastYear)
          AND b.organization_id = :organizationId
          AND b.isActive = true
          AND ps.payment_schedule_type = 'CUSTOMER' 
        GROUP BY YEAR(b.created_date), MONTH(b.created_date)
        ORDER BY year, month
    """,
            nativeQuery = true
    )
    List<Map<String , Object>> findMonthlyBookingStatsNativeByAmount(@Param("currentYear") int currentYear,
                                                             @Param("lastYear") int lastYear,
                                                             @Param("organizationId") long organizationId);



    @Query(
            value = """
        SELECT 
            MONTH(b.created_date) AS month,
             YEAR(b.created_date) AS year,
            SUM(ps.total_amount) AS amount 
        FROM booking b
        JOIN payment_schedule ps ON b.unit_id = ps.unit_id
        WHERE b.project_id = :projectId
        AND b.isActive = true 
          AND ps.payment_schedule_type = 'CUSTOMER' 
        GROUP BY YEAR(b.created_date), MONTH(b.created_date)
        ORDER BY year, month
    """,
            nativeQuery = true
    )
    List<Map<String , Object>> findMonthlyProjectSales(@Param("projectId") long projectId);


    @Query(
            value = """
        SELECT 
            MONTH(b.created_date) AS month,
            YEAR(b.created_date) AS year,
            SUM(ps.total_amount) AS amount 
        FROM booking b
        JOIN payment_schedule ps ON b.unit_id = ps.unit_id
        WHERE b.organization_id = :organizationId
        AND b.isActive = true
          AND ps.payment_schedule_type = 'CUSTOMER'
          AND b.created_date BETWEEN :startDate AND :endDate
        GROUP BY YEAR(b.created_date), MONTH(b.created_date)
        ORDER BY year, month
    """,
            nativeQuery = true
    )
    List<Map<String, Object>> findMonthlySalesByOrganizationAndDateRange(
            @Param("organizationId") long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


    @Query(
            value = """
        SELECT 
            MONTH(cp.updated_date) AS month,
            YEAR(cp.updated_date) AS year,
            SUM(cp.received_amount) AS amount
        FROM customer_account ca
        JOIN customer_payment cp ON ca.id = cp.customer_account_id
        WHERE ca.project_id = :projectId
        AND ca.isActive = true
        GROUP BY YEAR(cp.updated_date), MONTH(cp.updated_date)
        ORDER BY year, month
    """,
            nativeQuery = true
    )
    List<Map<String, Object>> findMonthlyProjectReceivedAmount(@Param("projectId") long projectId);


    @Query(
            value = """
                    
                       SELECT
                           MONTH(b.updated_date) AS month,
                           YEAR(b.updated_date) AS year,
                           COUNT(b.id) AS amount
                       FROM booking b
                       WHERE b.project_id = :projectId
                       AND b.isActive = true
                       GROUP BY YEAR(b.updated_date), MONTH(b.updated_date)
                       ORDER BY year, month;
    """,
            nativeQuery = true
    )
    List<Map<String, Object>> findMonthlyProjectClientCount(@Param("projectId") long projectId);




}
