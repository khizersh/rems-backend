package com.rem.backend.repository;

import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.customer.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface CustomerAccountRepo extends JpaRepository<CustomerAccount , Long> {


    Page<CustomerAccount> findByUnit_IdAndIsActiveTrue(Long unitId, Pageable pageable);
    Page<CustomerAccount> findByProject_ProjectIdAndIsActiveTrue(Long projectId, Pageable pageable);
    Page<CustomerAccount> findByProject_OrganizationIdAndIsActiveTrue(Long organizationId, Pageable pageable);
    Page<CustomerAccount> findByUnit_FloorIdAndIsActiveTrue(Long floorId, Pageable pageable);
    Page<CustomerAccount> findByCustomer_CustomerIdAndIsActiveTrue(Long customerId , Pageable pageable);




    @Query(value =  "SELECT ca.id AS accountId, c.name AS customerName , c.customer_id AS customerId,  u.serial_no    " +
            "AS unitSerial " +
            "FROM customer_account ca LEFT JOIN customer c ON ca.customer_id = c.customer_id " +
            " LEFT JOIN unit u ON ca.unit_id = u.id" +
            " where c.organization_id = :organizationId and ca.isActive = true" , nativeQuery = true)
    List<Map<String , Object>> findNameIdOrganization(Long organizationId);



    @Query(
            value = """
        SELECT 
            c.name        AS customerName,
            ca.id         AS accountId,
            c.customer_id AS customerId,
            u.serial_no   AS unitSerial
        FROM customer_account ca
        JOIN customer c ON ca.customer_id = c.customer_id
        JOIN unit u     ON ca.unit_id = u.id
        WHERE ca.project_id = :projectId
        and ca.isActive = true
        """,
            nativeQuery = true
    )
    List<Map<String , Object>> findNameIdProject(Long projectId);



    Page<CustomerAccount> findAllByOrderByCreatedDateDesc(Pageable pageable);

    Optional<CustomerAccount> findByCustomer_CustomerIdAndUnit_Id(Long customerId, Long unitId);





    @Query(value = """
    SELECT SUM(ca.total_amount)
    FROM customer_account ca
    JOIN project p ON ca.project_id = p.project_id
    WHERE p.organization_id = :organizationId
    AND ca.created_date >= :fromDate
    and ca.isActive = true
""", nativeQuery = true)
    Double getTotalAmountByOrganizationIdAndCreatedAfter(
            @Param("organizationId") Long organizationId,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query(value = """
    SELECT SUM(ca.total_amount)
    FROM customer_account ca
    JOIN project p ON ca.project_id = p.project_id
    WHERE p.project_id = :projectId
    and ca.isActive = true
""", nativeQuery = true)
    Double getTotalAmountSaleByProjectId(
            @Param("projectId") Long projectId
    );


    @Query(value = """
    SELECT SUM(cp.amount)
    FROM customer_payment cp
    JOIN customer_account ca ON cp.customer_account_id = ca.id
    JOIN project p ON ca.project_id = p.project_id
    WHERE p.project_id = :projectId
    and ca.isActive = true
""", nativeQuery = true)
    Double getTotalAmountReceivedByProjectId(
            @Param("projectId") Long projectId
    );






    @Query(value = """
    SELECT SUM(cp.amount)
    FROM customer_payment cp
    JOIN customer_account ca ON cp.customer_account_id = ca.id
    JOIN project p ON ca.project_id = p.project_id
    WHERE p.organization_id = :organizationId
    AND cp.created_date >= :fromDate
    and ca.isActive = true
""", nativeQuery = true)
    Double getTotalReceivedAmountByOrganizationIdAndDate(
            @Param("organizationId") Long organizationId,
            @Param("fromDate") LocalDateTime fromDate
    );



    @Query(value = """
            SELECT
            SUM(ca.total_balance_amount)  AS totalReceivable
            FROM customer_account ca
            JOIN project p ON ca.project_id = p.project_id
            WHERE p.organization_id = :organizationId
            and ca.isActive = true
""", nativeQuery = true)
    Double getTotalReceiveableAmountByOrganizationId(@Param("organizationId") Long organizationId);


//    @Query(value = """
//            SELECT
//            SUM(cp.amount) - SUM(cp.received_amount) AS totalReceivable
//            FROM customer_payment cp
//            JOIN customer_account ca ON cp.customer_account_id = ca.id
//            JOIN project p ON ca.project_id = p.project_id
//            WHERE p.organization_id = :organizationId
//""", nativeQuery = true)
//    Double getTotalReceiveableAmountByOrganizationId(@Param("organizationId") Long organizationId);



}
