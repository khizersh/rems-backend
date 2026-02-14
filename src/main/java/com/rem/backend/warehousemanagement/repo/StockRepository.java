package com.rem.backend.warehousemanagement.repo;

import com.rem.backend.warehousemanagement.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByWarehouseIdAndItemId(Long warehouseId, Long itemId);

    boolean existsByWarehouseIdAndItemId(Long warehouseId, Long itemId);

    List<Stock> findByWarehouseId(Long warehouseId);

    Page<Stock> findByWarehouseId(Long warehouseId, Pageable pageable);

    List<Stock> findByItemId(Long itemId);

    Page<Stock> findByItemId(Long itemId, Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE s.warehouseId = :warehouseId AND s.quantity > 0")
    List<Stock> findAvailableStockByWarehouse(@Param("warehouseId") Long warehouseId);

    @Query("SELECT s FROM Stock s WHERE s.itemId = :itemId AND s.quantity > 0")
    List<Stock> findAvailableStockByItem(@Param("itemId") Long itemId);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.itemId = :itemId")
    BigDecimal getTotalQuantityByItem(@Param("itemId") Long itemId);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.warehouseId = :warehouseId")
    BigDecimal getTotalQuantityByWarehouse(@Param("warehouseId") Long warehouseId);

    @Query("SELECT s FROM Stock s WHERE s.quantity <= :threshold")
    List<Stock> findLowStockItems(@Param("threshold") BigDecimal threshold);
}
