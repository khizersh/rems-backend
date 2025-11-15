package com.rem.backend.repository;

import com.rem.backend.entity.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {

    Page<Customer> findByOrganizationId(long organizationId, Pageable pageable);
//    Page<Customer> findByProjectId(long organizationId, Pageable pageable);
//    Page<Customer> findByFloorId(long organizationId, Pageable pageable);
//    Page<Customer> findByUnitId(long organizationId, Pageable pageable);
//    boolean existsByUnitId(long unitId);
    Optional<Customer> findByUserId(long userId);

    @Query(value = "SELECT c.customer_id AS customerId, c.name AS name " +
            "FROM customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) and c.organization_id = :orgId" , nativeQuery = true)
    List<Map<String , Object>> searchByName(String name, long orgId);

    @Query(value = "SELECT c.customer_id AS customerId, c.name AS name " +
            "FROM customer c where c.organization_id = :orgId ORDER BY c.created_date DESC" , nativeQuery = true)
    List<Map<String , Object>> findTop20ByOrderByCreatedDateDesc(long orgId);


//    @Query(
//            value = """
//        SELECT
//            c.customer_id     AS customerId,
//            c.national_id     AS nationalId,
//            c.name            AS customerName,
//            c.contact_no      AS contactNo,
//            c.guardian_name   AS guardianName,
//            c.address         AS customerAddress,
//            p.name            AS projectName,
//            p.project_id      AS projectId,
//            f.floor           AS floorNo,
//            u.serial_no       AS unitSerial,
//            u.unit_type       AS unitType
//        FROM customer c
//        JOIN project p ON  f.project_id = p.project_id
//        JOIN unit u    ON  u.id = :unitId
//        JOIN floor f   ON u.floor_id = f.id
//        WHERE c.customer_id = :customerId
//        """,
//            nativeQuery = true
//    )
//    Map<String, Object> getAllDetailsByCustomerId(@Param("customerId") long customerId , @Param("unitId") long unitId);


    @Query(
            value = """
        SELECT 
            c.customer_id     AS customerId,
            c.national_id     AS nationalId,
            c.name            AS customerName,
            c.contact_no      AS contactNo,
            c.guardian_name   AS guardianName,
            c.address         AS customerAddress,
            p.name            AS projectName,
            p.project_id      AS projectId,
            f.floor           AS floorNo,
            u.serial_no       AS unitSerial,
            u.unit_type       AS unitType
        FROM customer c
        JOIN unit u    ON  u.id = :unitId
        JOIN floor f   ON  u.floor_id = f.id
        JOIN project p ON  f.project_id = p.project_id
        WHERE c.customer_id = :customerId
        """,
            nativeQuery = true
    )
    Map<String, Object> getAllDetailsByCustomerId(@Param("customerId") long customerId ,
                                                  @Param("unitId") long unitId);



    long countByCreatedDateAfterAndOrganizationId(LocalDateTime date, long orgId);
    long countByOrganizationId(long orgId);

}
