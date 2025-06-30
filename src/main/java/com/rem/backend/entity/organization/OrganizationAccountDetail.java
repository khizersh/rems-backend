package com.rem.backend.entity.organizationAccount;

import com.rem.backend.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name = "organization_account_detail")
@Data
public class OrganizationAccountDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long organizationAcctId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String comments;

    private long projectId;


    private String projectName;

    private String customerName;

    private String unitSerialNo;

    private long customerId;

    private long customerPaymentId;

    private long customerPaymentDetailId;

    private long customerAccountId;

    private long expenseId;

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
