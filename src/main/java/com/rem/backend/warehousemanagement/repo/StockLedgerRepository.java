package com.rem.backend.warehousemanagement.repo;

import com.rem.backend.warehousemanagement.entity.StockLedger;
import com.rem.backend.enums.StockRefType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedger, Long> {

    Page<StockLedger> findByWarehouseIdOrderByTxnDateDesc(Long warehouseId, Pageable pageable);

    Page<StockLedger> findByItemIdOrderByTxnDateDesc(Long itemId, Pageable pageable);

    Page<StockLedger> findByWarehouseIdAndItemIdOrderByTxnDateDesc(Long warehouseId, Long itemId, Pageable pageable);

    List<StockLedger> findByRefTypeAndRefId(StockRefType refType, Long refId);

    Page<StockLedger> findByRefTypeAndRefId(StockRefType refType, Long refId, Pageable pageable);

    @Query("SELECT sl FROM StockLedger sl WHERE sl.warehouseId = :warehouseId AND sl.txnDate BETWEEN :startDate AND :endDate ORDER BY sl.txnDate DESC")
    Page<StockLedger> findByWarehouseAndDateRange(@Param("warehouseId") Long warehouseId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate,
                                                  Pageable pageable);

    @Query("SELECT sl FROM StockLedger sl WHERE sl.itemId = :itemId AND sl.txnDate BETWEEN :startDate AND :endDate ORDER BY sl.txnDate DESC")
    Page<StockLedger> findByItemAndDateRange(@Param("itemId") Long itemId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);

    @Query("SELECT sl FROM StockLedger sl WHERE sl.refType = :refType ORDER BY sl.txnDate DESC")
    Page<StockLedger> findByRefType(@Param("refType") StockRefType refType, Pageable pageable);
}
