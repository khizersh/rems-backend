package com.rem.backend.repository;

import com.rem.backend.entity.expense.Expense;
import com.rem.backend.enums.ExpenseType;
import com.rem.backend.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface ExpenseRepo extends JpaRepository<Expense, Long> {




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

//
//    Page<Expense> findAllByOrganizationId(long orgId, Pageable pageable);
//    Page<Expense> findAllByOrganizationIdAndExpenseType(long orgId,ExpenseType expenseType, Pageable pageable);
//
//    Page<Expense> findAllByVendorAccountIdAndExpenseType(long vendorAccountId, ExpenseType expenseType, Pageable pageable);
//
    List<Expense> findAllByVendorAccountId(long vendorAccountId);
//
//    Page<Expense> findAllByProjectIdAndExpenseType(long projectId, ExpenseType expenseType, Pageable pageable);
//
//
//    List<Expense> findAllByProjectId(long projectId);
//
//
//    Page<Expense> findAllByProjectIdAndVendorAccountIdAndExpenseType(long projectId, long vendorAccountId, ExpenseType expenseType, Pageable pageable);
//
//    Page<Expense> findByExpenseCOAIdAndOrganizationId(long expenseCOAId, long organizationId, Pageable pageable);
//
//    Page<Expense> findByOrganizationIdAndExpenseCOAIdIn(long orgId, List<Long> expenseCOAIds, Pageable pageable);
//    Page<Expense> findByOrganizationIdAndExpenseCOAIdIsNotNullOrExpenseTitle(long orgId , String expenseTitle, Pageable pageable);


    Page<Expense> findAllByOrganizationIdAndCreditAmountGreaterThan(
            long organizationId,
            Double amount,
            Pageable pageable
    );



    Page<Expense> findAllByOrganizationIdAndVendorAccountIdAndCreditAmountGreaterThan(
            long organizationId,
            long vendorAccountId,
            Double amount,
            Pageable pageable
    );


    Page<Expense> findAllByOrganizationIdAndCreatedDateBetween(
            long orgId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );


    Page<Expense> findAllByOrganizationIdAndPaymentStatusAndExpenseTypeAndCreatedDateBetween(
            long orgId,
            PaymentStatus paymentStatus,
            ExpenseType expenseType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );


    Page<Expense> findAllByOrganizationIdAndExpenseTypeAndCreatedDateBetween(
            long orgId,
            ExpenseType expenseType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    Page<Expense> findAllByVendorAccountIdAndPaymentStatusAndExpenseTypeAndCreatedDateBetween(
            long vendorAccountId,
            PaymentStatus paymentStatus,
            ExpenseType expenseType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );


    Page<Expense> findAllByVendorAccountIdAndExpenseTypeAndCreatedDateBetween(
            long vendorAccountId,
            ExpenseType expenseType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    List<Expense> findAllByVendorAccountIdAndCreatedDateBetween(
            long vendorAccountId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    Page<Expense> findAllByProjectIdAndPaymentStatusAndExpenseTypeAndCreatedDateBetween(
            long projectId,
            PaymentStatus paymentStatus,
            ExpenseType expenseType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );


    Page<Expense> findAllByProjectIdAndExpenseTypeAndCreatedDateBetween(
            long projectId,
            ExpenseType expenseType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    List<Expense> findAllByProjectId(
            long projectId
    );

    Page<Expense> findAllByProjectIdAndPaymentStatusAndVendorAccountIdAndExpenseTypeAndCreatedDateBetween(
            long projectId,
            PaymentStatus paymentStatus,
            long vendorAccountId,
            ExpenseType expenseType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );


    Page<Expense> findAllByProjectIdAndVendorAccountIdAndExpenseTypeAndCreatedDateBetween(
            long projectId,
            long vendorAccountId,
            ExpenseType expenseType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    Page<Expense> findByExpenseCOAIdAndOrganizationIdAndCreatedDateBetween(
            long expenseCOAId,
            long organizationId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    Page<Expense> findByOrganizationIdAndExpenseCOAIdInAndCreatedDateBetween(
            long orgId,
            List<Long> expenseCOAIds,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
    SELECT e FROM Expense e
    WHERE e.organizationId = :orgId
      AND e.createdDate BETWEEN :startDate AND :endDate
      AND (e.expenseCOAId != 0 OR e.expenseTitle = :expenseTitle)
      AND (e.expenseType = :expenseType)
""")
    Page<Expense> findByOrgAndCoaOrTitleWithinDate(
            @Param("orgId") long orgId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("expenseTitle") String expenseTitle,
            @Param("expenseType") ExpenseType expenseType,
            Pageable pageable
    );


}
