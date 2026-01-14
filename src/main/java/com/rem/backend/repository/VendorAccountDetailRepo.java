package com.rem.backend.repository;

import com.rem.backend.dto.analytic.OrganizationAccountDetailProjection;
import com.rem.backend.entity.vendor.VendorPayment;
import jakarta.transaction.Transactional;
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
public interface VendorAccountDetailRepo extends JpaRepository<VendorPayment, Long> {

    Page<VendorPayment> findByVendorAccountIdOrderByIdDesc(long acctId , Pageable pageable);
    List<VendorPayment> findByVendorAccountIdOrderByIdDesc(long acctId );
    List<VendorPayment> findByVendorAccountIdOrderByIdAsc(long acctId );
    Optional<VendorPayment> findByExpenseId(long expenseID );
    @Transactional
    void deleteByExpenseId(Long expenseId);


    @Query("""
    SELECT
        va.name                    AS accountName,
        'CREDIT'         AS transactionType,
        vp.creditAmount              AS amount,
        ''                          AS comments,
        ''                          AS projectName,
        ''                          AS customerName,
        ''                          AS unitSerialNo,
        vp.createdBy               AS createdBy,
        vp.updatedBy               AS updatedBy,
        vp.createdDate             AS createdDate,
        vp.updatedDate             AS updatedDate
    FROM VendorPayment vp
    JOIN VendorAccount va ON va.id = vp.vendorAccountId
    WHERE va.organizationId = :organizationId
      AND vp.createdDate BETWEEN :fromDate AND :toDate 
      AND vp.creditAmount > 0 Order by vp.createdDate Desc
""")
    Page<OrganizationAccountDetailProjection>
    findVendorPaymentsProjectionByOrganizationAndDateRangeWithoutPagination(
            @Param("organizationId") long organizationId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );



    @Query("""
    SELECT
        va.name           AS accountName,
        'CREDIT'          AS transactionType,
        vp.creditAmount   AS amount,
        ''                AS comments,
        ''                AS projectName,
        ''                AS customerName,
        ''                AS unitSerialNo,
        vp.createdBy      AS createdBy,
        vp.updatedBy      AS updatedBy,
        vp.createdDate    AS createdDate,
        vp.updatedDate    AS updatedDate
    FROM VendorPayment vp
    JOIN VendorAccount va ON va.id = vp.vendorAccountId
    WHERE va.organizationId = :organizationId
      AND vp.createdDate BETWEEN :fromDate AND :toDate
      AND vp.creditAmount > 0 Order by vp.createdDate Desc
""")
    List<OrganizationAccountDetailProjection>
    findVendorPaymentsProjectionByOrganizationAndDateRangeWithoutPagination(
            @Param("organizationId") long organizationId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
