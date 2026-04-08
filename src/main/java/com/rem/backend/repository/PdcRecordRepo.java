package com.rem.backend.repository;

import com.rem.backend.entity.pdc.PdcRecord;
import com.rem.backend.enums.PdcStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PdcRecordRepo extends JpaRepository<PdcRecord, Long> {

    // Find all PDC records by organization and status
    Page<PdcRecord> findAllByOrganizationIdAndStatus(
            Long organizationId,
            PdcStatus status,
            Pageable pageable
    );

    // Find PDC by cheque number (to prevent duplicates)
    Optional<PdcRecord> findByChequeNumberAndOrganizationId(String chequeNumber, Long organizationId);

    // PDC due today (chequeDate = today and status = PENDING)
    @Query("""
        SELECT p FROM PdcRecord p
        WHERE p.organizationId = :orgId
          AND p.status = 'PENDING'
          AND p.chequeDate = :today
        ORDER BY p.chequeDate ASC
    """)
    Page<PdcRecord> findPdcDueToday(
            @Param("orgId") Long orgId,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    // Overdue PDC (cheque date < today and still PENDING)
    @Query("""
        SELECT p FROM PdcRecord p
        WHERE p.organizationId = :orgId
          AND p.status = 'PENDING'
          AND p.chequeDate < :today
        ORDER BY p.chequeDate ASC
    """)
    Page<PdcRecord> findPdcOverdue(
            @Param("orgId") Long orgId,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    // Upcoming PDC (cheque date > today and status = PENDING)
    @Query("""
        SELECT p FROM PdcRecord p
        WHERE p.organizationId = :orgId
          AND p.status = 'PENDING'
          AND p.chequeDate > :today
        ORDER BY p.chequeDate ASC
    """)
    Page<PdcRecord> findPdcUpcoming(
            @Param("orgId") Long orgId,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    // Filter by vendor
    Page<PdcRecord> findAllByOrganizationIdAndStatusAndVendorAccountId(
            Long organizationId,
            PdcStatus status,
            Long vendorAccountId,
            Pageable pageable
    );

    // Filter by project
    Page<PdcRecord> findAllByOrganizationIdAndStatusAndProjectId(
            Long organizationId,
            PdcStatus status,
            Long projectId,
            Pageable pageable
    );

    // Get all PDCs linked to an expense
    Optional<PdcRecord> findByExpenseId(Long expenseId);
}
