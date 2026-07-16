package com.rem.backend.analytics.reporting.admin.query;

import com.rem.backend.propertymanagement.entity.PropertyAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Reporting aggregations over acquired property assets (land/plots), org-scoped.
 * Property asset status is AVAILABLE / USED (see {@code PropertyAssetStatus}).
 */
@Repository
public interface AdminPropertyReportRepository extends JpaRepository<PropertyAsset, Long> {

    @Query(value = """
            SELECT
                COUNT(*)                                                              AS totalAssets,
                COALESCE(SUM(CASE WHEN pa.status = 'AVAILABLE' THEN 1 ELSE 0 END), 0) AS availableAssets,
                COALESCE(SUM(CASE WHEN pa.status = 'USED' THEN 1 ELSE 0 END), 0)      AS usedAssets,
                COALESCE(SUM(pa.acquired_amount), 0)                                  AS totalAcquiredValue,
                COALESCE(SUM(CASE WHEN pa.status = 'AVAILABLE' THEN pa.acquired_amount ELSE 0 END), 0) AS availableValue
            FROM property_asset pa
            WHERE pa.organization_id = :organizationId
            """, nativeQuery = true)
    Map<String, Object> propertyAssetSnapshot(@Param("organizationId") long organizationId);
}
