package com.rem.backend.projectmanagement.repository;

import com.rem.backend.projectmanagement.entity.ProjectCost;
import com.rem.backend.projectmanagement.enums.ProjectCostCategory;
import com.rem.backend.projectmanagement.enums.ProjectCostStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProjectCostRepository extends JpaRepository<ProjectCost, Long> {

    Optional<ProjectCost> findByIdAndOrganizationId(Long id, Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ProjectCost c WHERE c.id = :id AND c.organizationId = :organizationId")
    Optional<ProjectCost> findByIdAndOrganizationIdForUpdate(
            @Param("id") Long id, @Param("organizationId") Long organizationId);

    Page<ProjectCost> findByProjectIdAndOrganizationId(Long projectId, Long organizationId, Pageable pageable);

    @Query("""
            SELECT c FROM ProjectCost c
            WHERE c.projectId = :projectId AND c.organizationId = :organizationId
              AND c.pendingAmount > 0 AND c.status IN :statuses
            """)
    Page<ProjectCost> findPayableCosts(
            @Param("projectId") Long projectId,
            @Param("organizationId") Long organizationId,
            @Param("statuses") java.util.Collection<ProjectCostStatus> statuses,
            Pageable pageable);

    @Query("""
            SELECT c FROM ProjectCost c
            WHERE c.projectId = :projectId AND c.organizationId = :organizationId
              AND (:category IS NULL OR c.category = :category)
              AND (:status IS NULL OR c.status = :status)
              AND (:vendorId IS NULL OR c.vendorId = :vendorId)
            """)
    Page<ProjectCost> filter(
            @Param("projectId") Long projectId,
            @Param("organizationId") Long organizationId,
            @Param("category") ProjectCostCategory category,
            @Param("status") ProjectCostStatus status,
            @Param("vendorId") Long vendorId,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(c.totalAmount), 0) FROM ProjectCost c
            WHERE c.projectId = :projectId AND c.organizationId = :organizationId
              AND c.category = :category AND c.status <> :cancelled
            """)
    BigDecimal sumActiveByCategory(
            @Param("projectId") Long projectId,
            @Param("organizationId") Long organizationId,
            @Param("category") ProjectCostCategory category,
            @Param("cancelled") ProjectCostStatus cancelled);

    @Query("""
            SELECT COALESCE(SUM(c.paidAmount), 0) FROM ProjectCost c
            WHERE c.projectId = :projectId AND c.organizationId = :organizationId
              AND c.status <> :cancelled
            """)
    BigDecimal sumPaidByProject(
            @Param("projectId") Long projectId,
            @Param("organizationId") Long organizationId,
            @Param("cancelled") ProjectCostStatus cancelled);

    @Query("""
            SELECT COALESCE(SUM(c.pendingAmount), 0) FROM ProjectCost c
            WHERE c.projectId = :projectId AND c.organizationId = :organizationId
              AND c.status <> :cancelled
            """)
    BigDecimal sumOutstandingByProject(
            @Param("projectId") Long projectId,
            @Param("organizationId") Long organizationId,
            @Param("cancelled") ProjectCostStatus cancelled);

    @Query("""
            SELECT COALESCE(SUM(c.pendingAmount), 0) FROM ProjectCost c
            WHERE c.projectId = :projectId
              AND c.organizationId = :organizationId
              AND c.status = :status
            """)
    BigDecimal sumPendingAmountByProjectIdAndOrganizationIdAndStatus(
            @Param("projectId") Long projectId,
            @Param("organizationId") Long organizationId,
            @Param("status") ProjectCostStatus status);
}
