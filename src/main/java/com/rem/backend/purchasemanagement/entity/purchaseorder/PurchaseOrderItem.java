package com.rem.backend.purchasemanagement.entity.purchaseorder;

import com.rem.backend.purchasemanagement.entity.items.Items;
import com.rem.backend.purchasemanagement.entity.items.ItemsUnit;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_order_item")
@Data
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long poId;

    @ManyToOne
    private Items items;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private Double rate;

    @Column(nullable = false)
    private Double amount;

    private Double receivedQuantity;

    private Double invoicedQuantity;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;


    @Transient
    private long itemsId;
}
