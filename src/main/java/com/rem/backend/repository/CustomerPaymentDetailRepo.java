package com.rem.backend.repository;

import com.rem.backend.entity.customer.CustomerPaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public interface CustomerPaymentDetailRepo extends JpaRepository<CustomerPaymentDetail , Long> {

    List<CustomerPaymentDetail> findByCustomerPaymentId(long customerPaymentId);

    // Dashboard query - payment mode distribution
    @Query(value = """
        SELECT 
            cpd.payment_type as paymentMode,
            COALESCE(SUM(cpd.amount), 0) as totalAmount,
            COUNT(cpd.id) as transactionCount
        FROM customer_payment_detail cpd
        JOIN customer_payment cp ON cpd.customer_payment_id = cp.id
        JOIN customer_account ca ON cp.customer_account_id = ca.id
        WHERE ca.customer_id = :customerId
        AND ca.is_active = 1
        GROUP BY cpd.payment_type
        ORDER BY totalAmount DESC
    """, nativeQuery = true)
    List<Map<String, Object>> getPaymentModeDistributionByCustomerId(@Param("customerId") Long customerId);

    List<CustomerPaymentDetail> findByCustomerPaymentIdIn(List<Long> paymentIds);
}
