package com.rem.backend.repository;

import com.rem.backend.dto.analytic.OrganizationAccountDetailProjection;
import com.rem.backend.entity.project.Project;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.rem.backend.entity.organizationAccount.OrganizationAccountDetail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationAccountDetailRepo extends JpaRepository<OrganizationAccountDetail, Long > {

    Page<OrganizationAccountDetail> findByOrganizationAcctId(long orgAcctId , Pageable pageable);

    Optional<OrganizationAccountDetail> findByExpenseId(long expenseId);

    @Transactional
    void deleteByExpenseId(Long expenseId);


    @Query("""
    SELECT a.name AS accountName,
           d.transactionType AS transactionType,
           d.amount AS amount,
           d.comments AS comments,
           d.projectName AS projectName,
           d.customerName AS customerName,
           d.unitSerialNo AS unitSerialNo,
           d.createdBy AS createdBy,
           d.updatedBy AS updatedBy,
           d.createdDate AS createdDate,
           d.updatedDate AS updatedDate
    FROM OrganizationAccountDetail d
    JOIN OrganizationAccount a ON d.organizationAcctId = a.id
    WHERE a.organizationId = :organizationId
      AND d.createdDate BETWEEN :startDate AND :endDate
    ORDER BY d.createdDate DESC
""")
    Page<OrganizationAccountDetailProjection> findAllByOrganizationIdAndDateRange(
            @Param("organizationId") long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );


    @Query("""
     SELECT a.name AS accountName,
           d.transactionType AS transactionType,
           d.amount AS amount,
           d.comments AS comments,
           d.projectName AS projectName,
           d.customerName AS customerName,
           d.unitSerialNo AS unitSerialNo,
           d.createdBy AS createdBy,
           d.updatedBy AS updatedBy,
           d.createdDate AS createdDate,
           d.updatedDate AS updatedDate
        FROM OrganizationAccountDetail d
        JOIN OrganizationAccount a ON d.organizationAcctId = a.id
        WHERE a.organizationId = :organizationId
          AND a.id = :organizationAcctId
          AND d.createdDate BETWEEN :startDate AND :endDate
        ORDER BY d.createdDate DESC
    """)
    Page<OrganizationAccountDetailProjection> findAllByOrgAndAccountAndDateRange(
            @Param("organizationId") long organizationId,
            @Param("organizationAcctId") Long organizationAcctId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
