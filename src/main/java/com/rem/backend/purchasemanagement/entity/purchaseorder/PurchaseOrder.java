package com.rem.backend.purchasemanagement.entity.purchaseorder;

import com.rem.backend.purchasemanagement.enums.PoStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_order")
@Data
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String poNumber;


    @Column(nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long vendorId;

    @Column(nullable = false)
    private Double totalAmount;


    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PoStatus status; // OPEN, PARTIAL, CLOSED, CANCELLED

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;
}
