package com.rem.backend.repository;

import com.rem.backend.entity.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
