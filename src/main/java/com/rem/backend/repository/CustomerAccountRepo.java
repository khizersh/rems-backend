package com.rem.backend.repository;

import com.rem.backend.entity.customer.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface CustomerAccountRepo extends JpaRepository<CustomerAccount , Long> {


    Page<CustomerAccount> findByUnit_Id(Long unitId, Pageable pageable);
    Page<CustomerAccount> findByProject_ProjectId(Long projectId, Pageable pageable);
    Page<CustomerAccount> findByProject_OrganizationId(Long organizationId, Pageable pageable);
    Page<CustomerAccount> findByUnit_FloorId(Long floorId, Pageable pageable);

    Optional<CustomerAccount> findByCustomer_CustomerId(Long customerId);




    @Query(value =  "SELECT ca.id AS accountId, c.name AS customerName , c.customer_id AS customerId FROM customer_account ca LEFT JOIN customer c ON ca.customer_id = c.customer_id where c.organization_id = :organizationId" , nativeQuery = true)
    List<Map<String , Object>> findNameIdOrganization(Long organizationId);


    @Query(value =  " SELECT c.name AS customerName, ca.id AS accountId, , c.customer_id AS customerId FROM customer_account ca JOIN customer c ON ca.customer_id = c.customer_id WHERE ca.project_id = :projectId;" , nativeQuery = true)
    List<Map<String , Object>> findNameIdProject(Long projectId);



    Page<CustomerAccount> findAllByOrderByCreatedDateDesc(Pageable pageable);

    Optional<CustomerAccount> findByCustomer_CustomerIdAndUnit_Id(Long customerId, Long unitId);

}
