package com.rem.backend.analytics.reporting.admin.query;

import com.rem.backend.projectmanagement.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Reporting aggregations over sellable inventory (units). Org scope is reached by walking
 * unit -&gt; floor -&gt; project (join convention reused from existing native queries).
 * <p>
 * Availability is modelled by the {@code is_booked} flag: 0 = available, 1 = booked/reserved.
 */
@Repository
public interface AdminInventoryReportRepository extends JpaRepository<Unit, Long> {

    /** Unit counts split by availability + total inventory valuation, in one row. */
    @Query(value = """
            SELECT
                COUNT(*)                                                   AS totalUnits,
                COALESCE(SUM(CASE WHEN u.is_booked = 0 THEN 1 ELSE 0 END), 0) AS availableUnits,
                COALESCE(SUM(CASE WHEN u.is_booked = 1 THEN 1 ELSE 0 END), 0) AS bookedUnits,
                COALESCE(SUM(u.amount), 0)                                  AS totalValuation,
                COALESCE(SUM(CASE WHEN u.is_booked = 0 THEN u.amount ELSE 0 END), 0) AS availableValuation
            FROM unit u
            JOIN floor f   ON u.floor_id = f.id
            JOIN project p ON f.project_id = p.project_id
            WHERE p.organization_id = :organizationId
            """, nativeQuery = true)
    Map<String, Object> inventorySnapshot(@Param("organizationId") long organizationId);
}
