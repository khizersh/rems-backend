package com.rem.backend.entity.customer;

import com.rem.backend.entity.organization.OrganizationAccountDetail;
import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.enums.PaymentType;
import jakarta.persistence.*;
import lombok.Data;

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

    @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isPaymentAddedToAccount = false;


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
        if (this.createdDate == null) {
            this.createdDate = LocalDateTime.now();
        }
        if (this.updatedDate == null) {
            this.updatedDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.updatedDate == null) {
            this.updatedDate = LocalDateTime.now();
        }
        if (this.paidDate == null) {
            this.paidDate = LocalDateTime.now();
        }
    }
}