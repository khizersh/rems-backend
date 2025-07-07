package com.rem.backend.entity.customer;

import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.enums.PaymentType;
import jakarta.persistence.*;
import lombok.Data;
import com.rem.backend.entity.organizationAccount.OrganizationAccountDetail;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customer_payment")
@Data
public class CustomerPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private int serialNo;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private double receivedAmount;

    @Column(nullable = false)
    private double remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private long customerAccountId;



    @Transient
    private List<CustomerPaymentDetail> customerPaymentDetails;

    @Transient
    private List<OrganizationAccountDetail> organizationAccountDetails;

    @Column(nullable = true)
    private LocalDateTime paidDate;

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