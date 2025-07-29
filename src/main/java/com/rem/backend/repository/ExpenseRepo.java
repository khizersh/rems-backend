package com.rem.backend.repository;

import com.rem.backend.entity.expense.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ExpenseRepo extends JpaRepository<Expense, Long> {

    Page<Expense> findAllByOrganizationId(long orgId, Pageable pageable);

    Page<Expense> findAllByVendorAccountId(long vendorAccountId, Pageable pageable);

    Page<Expense> findAllByProjectId(long projectId, Pageable pageable);

    List<Expense> findAllByProjectId(long projectId);


    Page<Expense> findAllByProjectIdAndVendorAccountId(long projectId, long vendorAccountId, Pageable pageable);


    @Query(
            value = """
                                     SELECT
                                                     YEAR(e.created_date) AS year,
                                                     MONTH(e.created_date) AS month,
                                                     SUM(e.total_amount) AS amount
                                                     FROM expense e
                                                     WHERE e.project_id = :projectId
                                                     GROUP BY YEAR(e.created_date), MONTH(e.created_date)
                                                     ORDER BY year, month;
                    """,
            nativeQuery = true
    )
    List<Map<String, Object>> findMonthlyProjectExpensePurchased(@Param("projectId") long projectId);


    @Query(
            value = """
                    
                    SELECT
                        YEAR(ed.created_date) AS year,
                        MONTH(ed.created_date) AS month,
                        SUM(ed.amount_paid) AS amount
                    FROM expense_detail ed
                    LEFT JOIN expense e ON e.id = ed.expense_id
                    WHERE e.project_id = :projectId
                    GROUP BY YEAR(ed.created_date), MONTH(ed.created_date)
                    ORDER BY year, month;
                    """,
            nativeQuery = true
    )
    List<Map<String, Object>> findMonthlyProjectExpensePaid(@Param("projectId") long projectId);


    @Query(
            value = """
                    SELECT
                        YEAR(e.updated_date) AS year,
                        MONTH(e.updated_date) AS month,
                        SUM(e.credit_amount) AS amount
                    FROM expense e
                    WHERE e.project_id = :projectId
                    GROUP BY YEAR(e.updated_date), MONTH(e.updated_date)
                    ORDER BY year, month;	
                    """,
            nativeQuery = true
    )
    List<Map<String, Object>> findMonthlyProjectExpenseCredit(@Param("projectId") long projectId);


    @Query(value = """
    SELECT 
        SUM(e.total_amount) AS totalAmount,
        SUM(e.amount_paid) AS amountPaid,
        SUM(e.credit_amount) AS creditAmount
    FROM expense e
    WHERE e.organization_id = :orgId
      AND e.created_date >= NOW() - INTERVAL :days DAY
    """, nativeQuery = true)
    Map<String , Object> getExpenseSumsByOrgAndDays(@Param("orgId") Long orgId, @Param("days") int days);

}
