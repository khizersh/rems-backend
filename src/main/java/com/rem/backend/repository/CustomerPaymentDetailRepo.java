package com.rem.backend.repository;

import com.rem.backend.entity.customer.CustomerPaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerPaymentDetailRepo extends JpaRepository<CustomerPaymentDetail , Long> {

    List<CustomerPaymentDetail> findByCustomerPaymentId(long customerPaymentId);
}
