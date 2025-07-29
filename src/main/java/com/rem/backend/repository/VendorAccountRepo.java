package com.rem.backend.repository;

import com.rem.backend.entity.vendor.VendorAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

@Repository
public interface VendorAccountRepo extends JpaRepository<VendorAccount , Long> {

    List<VendorAccount> findByNameContainingIgnoreCase(String name);
    Page<VendorAccount> findAllByOrganizationId(long organizationId, Pageable pageable);

    @Query(value =  " SELECT id , name FROM vendor_account  WHERE organization_id = :organizationId ;" , nativeQuery = true)
    List<Map<String , Object>> findAllByOrgId(long organizationId);


    @Query(value =  " SELECT sum(va.total_credit_amount) FROM vendor_account va WHERE organization_id = :organizationId ;" , nativeQuery = true)
    double findTotalPayableByOrgId(long organizationId);
}
