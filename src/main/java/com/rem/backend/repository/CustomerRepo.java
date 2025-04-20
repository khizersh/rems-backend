package com.rem.backend.repository;

import com.rem.backend.entity.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {

    Page<Customer> findByOrganizationId(long organizationId, Pageable pageable);
    Page<Customer> findByProjectId(long organizationId, Pageable pageable);
    Page<Customer> findByFloorId(long organizationId, Pageable pageable);
    Page<Customer> findByUnitId(long organizationId, Pageable pageable);
    boolean existsByUnitId(long unitId);
}
