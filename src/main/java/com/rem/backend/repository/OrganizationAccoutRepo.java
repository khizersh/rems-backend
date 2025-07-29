package com.rem.backend.repository;

import com.rem.backend.entity.organization.OrganizationAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import com.rem.backend.entity.organizationAccount.OrganizationAccountDetail;
@Repository
public interface OrganizationAccoutRepo extends JpaRepository<OrganizationAccount , Long> {

    List<OrganizationAccount> findByOrganizationId(long orgId);

    @Query("SELECT SUM(o.totalAmount) FROM OrganizationAccount o WHERE o.organizationId = :organizationId")
    Double getTotalAmountByOrganizationId(@Param("organizationId") Long organizationId);


    @Query("SELECT SUM(o.totalAmount) FROM OrganizationAccount o WHERE o.organizationId = :organizationId AND o.id = :accountId")
    Double getTotalAmountByOrganizationIdAndAccountId(@Param("organizationId") Long organizationId, @Param("accountId") Long accountId);


    @Query(value = """
    SELECT sum(d.amount)
    FROM organization_account_detail d
    JOIN organization_account a ON d.organization_acct_id = a.id
    WHERE a.organization_id = :organizationId
      AND d.transaction_type = :transactionType
      AND d.created_date BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    Double findSumBetweenDateByTransactionType(
            @Param("organizationId") Long organizationId,
            @Param("transactionType") String transactionType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(
            value = """
        SELECT d.* 
        FROM organization_account_detail d
        JOIN organization_account a ON d.organization_acct_id = a.id
        WHERE a.organization_id = :organizationId
          AND d.transaction_type = :transactionType
          AND d.created_date BETWEEN :startDate AND :endDate
        """,
            countQuery = """
        SELECT COUNT(*) 
        FROM organization_account_detail d
        JOIN organization_account a ON d.organization_acct_id = a.id
        WHERE a.organization_id = :organizationId
          AND d.transaction_type = :transactionType
          AND d.created_date BETWEEN :startDate AND :endDate
        """,
            nativeQuery = true
    )
    Page<OrganizationAccountDetail> findTransactionsByOrgIdAndTypeAndDateRange(
            @Param("organizationId") Long organizationId,
            @Param("transactionType") String transactionType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );


    @Query(value = """
  SELECT SUM(d.amount)
    FROM organization_account_detail d
    JOIN organization_account a ON d.organization_acct_id = a.id
    WHERE a.organization_id = :organizationId
      AND d.created_date BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    Double findSumBetweenDate(
            @Param("organizationId") Long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(
            value = """
        SELECT d.* 
        FROM organization_account_detail d
        JOIN organization_account a ON d.organization_acct_id = a.id
        WHERE a.organization_id = :organizationId
          AND d.created_date BETWEEN :startDate AND :endDate
        """,
            countQuery = """
        SELECT COUNT(*) 
        FROM organization_account_detail d
        JOIN organization_account a ON d.organization_acct_id = a.id
        WHERE a.organization_id = :organizationId
          AND d.created_date BETWEEN :startDate AND :endDate
        """,
            nativeQuery = true
    )
    Page<OrganizationAccountDetail> findTransactionsByOrgIdAndDateRange(
            @Param("organizationId") Long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

}
