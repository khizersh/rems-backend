package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.grn.Grn;
import com.rem.backend.purchasemanagement.enums.GrnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrnRepo extends JpaRepository<Grn, Long> {

    List<Grn> findByPoId(Long poId);

    Optional<Grn> findTopByOrderByIdDesc();

    Page<Grn> findByPoId(Long poId, Pageable pageable);

    @Query(value = """
        SELECT 
            g.po_id as poId,
            po.po_number as poNumber,
            po.vendor_id as vendorId,
            v.vendor_name as vendorName,
            po.project_id as projectId,
            p.project_name as projectName,
            po.po_date as poDate,
            COUNT(g.id) as totalGrnCount,
            MAX(g.received_date) as lastGrnDate,
            MIN(g.received_date) as firstGrnDate
        FROM grn g 
        INNER JOIN po po ON g.po_id = po.id 
        LEFT JOIN vendor_account v ON po.vendor_id = v.id
        LEFT JOIN project p ON po.project_id = p.project_id
        WHERE g.org_id = :orgId
        GROUP BY g.po_id, po.po_number, po.vendor_id, v.vendor_name, po.project_id, p.project_name, po.po_date
        ORDER BY MAX(g.received_date) DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT g.po_id) 
        FROM grn g 
        WHERE g.org_id = :orgId
        """,
        nativeQuery = true)
    Page<Object[]> findGrnGroupedByPoId(@Param("orgId") Long orgId, Pageable pageable);

    // Get GRNs with conditional filters (all parameters optional)
    @Query("SELECT g FROM Grn g WHERE " +
           "(:poId IS NULL OR g.poId = :poId) " +
           "AND (:vendorId IS NULL OR g.vendorId = :vendorId) " +
           "AND (:status IS NULL OR g.status = :status) " +
           "AND (:startDate IS NULL OR g.createdDate >= :startDate) " +
           "AND (:endDate IS NULL OR g.createdDate <= :endDate) " +
           "ORDER BY g.createdDate DESC")
    Page<Grn> findByConditionalFilters(
            @Param("poId") Long poId,
            @Param("vendorId") Long vendorId,
            @Param("status") GrnStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
