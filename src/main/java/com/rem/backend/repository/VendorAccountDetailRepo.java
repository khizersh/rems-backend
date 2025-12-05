package com.rem.backend.repository;

import com.rem.backend.entity.vendor.VendorPayment;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorAccountDetailRepo extends JpaRepository<VendorPayment, Long> {

    Page<VendorPayment> findByVendorAccountId(long acctId , Pageable pageable);
    List<VendorPayment> findByVendorAccountIdOrderByIdDesc(long acctId );
    Optional<VendorPayment> findByExpenseId(long expenseID );
    @Transactional
    void deleteByExpenseId(Long expenseId);
}
