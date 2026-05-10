package com.rem.backend.propertymanagement.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "property_purchase")
@Data
public class PropertyPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long organizationId;

    @Column(nullable = false)
    private long propertySellerId;

    @Column(nullable = false)
    private double totalAmount;

    @Column(nullable = false)
    private double paidAmount = 0.0;

    @Column(nullable = false)
    private double remainingAmount = 0.0;

    @Column(nullable = true)
    private String remarks;

    @Column(nullable = true)
    private String referenceNo;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        if (this.createdDate == null) {
            this.createdDate = LocalDateTime.now();
        }
        if (this.updatedDate == null) {
            this.updatedDate = LocalDateTime.now();
        }
        if (this.remainingAmount == 0.0 && this.totalAmount > 0) {
            this.remainingAmount = Math.max(0.0, this.totalAmount - this.paidAmount);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}

