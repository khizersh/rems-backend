package com.rem.backend.repository;

import com.rem.backend.entity.customer.CustomerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

@Repository
public interface CustomerPaymentRepo extends JpaRepository<CustomerPayment , Long> {

    Page<CustomerPayment> findByCustomerAccountId(long customerAccountId, Pageable pageable);
    List<CustomerPayment> findByCustomerAccountId(long customerAccountId);
    @Query("SELECT COALESCE(SUM(cp.receivedAmount), 0) FROM CustomerPayment cp WHERE cp.customerAccountId = :customerAccountId")
    double getTotalReceivedAmountByCustomerAccountId(@Param("customerAccountId") long customerAccountId);

    // Dashboard queries
    @Query(value = """
        SELECT 
            MONTH(cp.paid_date) as month,
            YEAR(cp.paid_date) as year,
            COALESCE(SUM(cp.received_amount), 0) as totalPaid
        FROM customer_payment cp
        JOIN customer_account ca ON cp.customer_account_id = ca.id
        WHERE ca.customer_id = :customerId
        AND cp.payment_status != 'UNPAID'
        AND cp.paid_date IS NOT NULL
        AND ca.is_active = 1
        GROUP BY YEAR(cp.paid_date), MONTH(cp.paid_date)
        ORDER BY YEAR(cp.paid_date) DESC, MONTH(cp.paid_date) DESC
    """, nativeQuery = true)
    List<Map<String, Object>> getMonthlyPaymentsByCustomerId(@Param("customerId") Long customerId);

    @Query(value = """
        SELECT cp.*
        FROM customer_payment cp
        JOIN customer_account ca ON cp.customer_account_id = ca.id
        WHERE ca.customer_id = :customerId
        AND ca.is_active = 1
        ORDER BY cp.paid_date DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<CustomerPayment> getRecentPaymentsByCustomerId(@Param("customerId") Long customerId, @Param("limit") int limit);

    List<CustomerPayment> findByCustomerAccountIdIn(List<Long> accountIds);
}
