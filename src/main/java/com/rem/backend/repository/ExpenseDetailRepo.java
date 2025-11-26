package com.rem.backend.repository;

import com.rem.backend.entity.expense.ExpenseDetail;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseDetailRepo extends JpaRepository<ExpenseDetail , Long> {


    List<ExpenseDetail> findByExpenseIdOrderByCreatedDateDesc(long expenseId);

    @Transactional
    void deleteByExpenseId(Long expenseId);
}
