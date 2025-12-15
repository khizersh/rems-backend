package com.rem.backend.entity.vendor;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name = "vendor_account")
@Data
public class VendorAccount {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long organizationId;





    @Column(nullable = true)
    private Long historyExpenseId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double totalAmountPaid;

    @Column(nullable = false)
    private double totalCreditAmount;

    @Column(nullable = false)
    private double totalBalanceAmount;

    @Column(nullable = false)
    private double totalAmount;

    @Column(nullable = false)
    private LocalDateTime lastUpdatedDateTime;

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
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
        this.lastUpdatedDateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
        this.lastUpdatedDateTime = LocalDateTime.now();
    }
}
