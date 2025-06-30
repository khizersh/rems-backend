package com.rem.backend.repository;

import com.rem.backend.entity.vendor.VendorPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorAccountDetailRepo extends JpaRepository<VendorPayment, Long> {

    Page<VendorPayment> findByVendorAccountId(long acctId , Pageable pageable);
}
