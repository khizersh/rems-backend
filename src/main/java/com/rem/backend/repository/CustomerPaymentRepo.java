package com.rem.backend.repository;

import com.rem.backend.entity.customer.CustomerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface CustomerPaymentRepo extends JpaRepository<CustomerPayment , Long> {

    Page<CustomerPayment> findByCustomerAccountId(long customerAccountId, Pageable pageable);
}
