package com.rem.backend.repository;

import com.rem.backend.entity.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {

    Page<Customer> findByOrganizationId(long organizationId, Pageable pageable);
    Page<Customer> findByProjectId(long organizationId, Pageable pageable);
    Page<Customer> findByFloorId(long organizationId, Pageable pageable);
    Page<Customer> findByUnitId(long organizationId, Pageable pageable);
    boolean existsByUnitId(long unitId);

    @Query(value = "SELECT c.customer_id AS customerId, c.name AS name, c.unit_id AS unitId " +
            "FROM customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))" , nativeQuery = true)
    List<Map<String , Object>> searchByName(String name);

    @Query(value = "SELECT c.customer_id AS customerId, c.name AS name, c.unit_id AS unitId " +
            "FROM customer c ORDER BY c.created_date DESC" , nativeQuery = true)
    List<Map<String , Object>> findTop20ByOrderByCreatedDateDesc();


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
            f.floor           AS floorNo,
            u.serial_no       AS unitSerial,
            u.unit_type       AS unitType
        FROM customer c
        JOIN project p ON c.project_id = p.project_id
        JOIN floor f   ON c.floor_id = f.id
        JOIN unit u    ON c.unit_id = u.id
        WHERE c.customer_id = :customerId
        """,
            nativeQuery = true
    )
    Map<String, Object> getAllDetailsByCustomerId(@Param("customerId") long customerId);

}
