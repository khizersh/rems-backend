package com.rem.backend.repository;

import com.rem.backend.entity.customer.CustomerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPaymentRepo extends JpaRepository<CustomerPayment , Long> {
}
