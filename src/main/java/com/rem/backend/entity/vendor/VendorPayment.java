package com.rem.backend.entity.vendor;


import com.rem.backend.enums.TransactionType;
import com.rem.backend.enums.VendorPaymentType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name = "vendor_payment")
@Data
public class VendorPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private double vendorAccountId;


    @Column(nullable = true)
    private Long organizationAccountId;

    @Column(nullable = false)
    private double projectId;

    @Column(nullable = false)
    private double amountPaid;

    @Column(nullable = false)
    private double creditAmount;


    @Column(nullable = false)
    private double balanceAmount;

    @Column(nullable = false)
    private double materialAmount;


    @Column(nullable = true )
    private long expenseId = 0;

    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private VendorPaymentType vendorPaymentType;

    @Transient
    private String organizationAccount;

    @Transient
    private String vendorAccount;


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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
