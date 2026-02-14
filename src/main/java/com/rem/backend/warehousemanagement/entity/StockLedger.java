package com.rem.backend.warehousemanagement.entity;

import com.rem.backend.enums.StockRefType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_ledger", indexes = {
    @Index(name = "idx_warehouse_id", columnList = "warehouseId"),
    @Index(name = "idx_item_id", columnList = "itemId"),
    @Index(name = "idx_ref_type_ref_id", columnList = "refType,refId"),
    @Index(name = "idx_txn_date", columnList = "txnDate")
})
@Data
public class StockLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private Long itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockRefType refType;

    @Column(nullable = false)
    private Long refId;

    @Column(nullable = false)
    private LocalDateTime txnDate;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal qtyIn = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal qtyOut = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    @Column(nullable = true, precision = 15, scale = 4)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(nullable = true, precision = 15, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = true)
    private String remarks;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.txnDate == null) {
            this.txnDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
