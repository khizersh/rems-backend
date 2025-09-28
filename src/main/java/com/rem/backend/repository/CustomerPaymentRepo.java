package com.rem.backend.repository;

import com.rem.backend.entity.customer.CustomerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface CustomerPaymentRepo extends JpaRepository<CustomerPayment , Long> {

    Page<CustomerPayment> findByCustomerAccountId(long customerAccountId, Pageable pageable);
    List<CustomerPayment> findByCustomerAccountId(long customerAccountId);
    @Query("SELECT COALESCE(SUM(cp.receivedAmount), 0) FROM CustomerPayment cp WHERE cp.customerAccountId = :customerAccountId")
    double getTotalReceivedAmountByCustomerAccountId(@Param("customerAccountId") long customerAccountId);
}
