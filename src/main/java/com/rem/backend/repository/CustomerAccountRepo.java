package com.rem.backend.repository;

import com.rem.backend.entity.customer.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface CustomerAccountRepo extends JpaRepository<CustomerAccount , Long> {


    Page<CustomerAccount> findByProject_ProjectId(Long projectId, Pageable pageable);

    Page<CustomerAccount> findByCustomer_CustomerId(Long customerId, Pageable pageable);

    Page<CustomerAccount> findByUnit_UnitId(Long unitId, Pageable pageable);

    Page<CustomerAccount> findByProject_ProjectIdAndCustomer_OrganizationId(Long projectId, Long organizationId, Pageable pageable);

    Page<CustomerAccount> findAllByOrderByCreatedDateDesc(Pageable pageable);

}
