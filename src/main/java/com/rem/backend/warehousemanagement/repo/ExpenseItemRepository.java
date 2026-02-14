package com.rem.backend.warehousemanagement.repo;

import com.rem.backend.warehousemanagement.entity.ExpenseItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseItemRepository extends JpaRepository<ExpenseItem, Long> {

    List<ExpenseItem> findByExpenseId(Long expenseId);

    Page<ExpenseItem> findByExpenseId(Long expenseId, Pageable pageable);

    List<ExpenseItem> findByItemId(Long itemId);

    List<ExpenseItem> findByWarehouseId(Long warehouseId);

    List<ExpenseItem> findByStockEffectTrue();

    @Query("SELECT ei FROM ExpenseItem ei WHERE ei.expenseId = :expenseId AND ei.stockEffect = true")
    List<ExpenseItem> findStockEffectItemsByExpense(@Param("expenseId") Long expenseId);

    @Query("SELECT ei FROM ExpenseItem ei WHERE ei.warehouseId = :warehouseId AND ei.stockEffect = true")
    List<ExpenseItem> findStockEffectItemsByWarehouse(@Param("warehouseId") Long warehouseId);

    void deleteByExpenseId(Long expenseId);
}
