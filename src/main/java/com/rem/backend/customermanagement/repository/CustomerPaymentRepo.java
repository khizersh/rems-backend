package com.rem.backend.customermanagement.repository;

import com.rem.backend.customermanagement.entity.CustomerPayment;
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
            MONTH(cpd.created_date) as month,
            YEAR(cpd.created_date) as year,
            COALESCE(SUM(cpd.amount), 0) as totalPaid
        FROM customer_payment cp
        JOIN customer_payment_detail cpd ON cp.id = cpd.customer_payment_id
        JOIN customer_account ca ON cp.customer_account_id = ca.id
        WHERE ca.customer_id = :customerId
        AND cp.payment_status != 'UNPAID'
        AND cpd.created_date IS NOT NULL
        AND ca.is_active = 1
        GROUP BY YEAR(cpd.created_date), MONTH(cpd.created_date)
        ORDER BY YEAR(cpd.created_date) DESC, MONTH(cpd.created_date) DESC
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
