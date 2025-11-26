package com.rem.backend.entity.customerpayable;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_payable_details")
public class CustomerPayableDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_payable_id", nullable = false)
    private CustomerPayable customerPayable;

    @Column(nullable = false)
    private String type; // REFUND, DEDUCTION, INSTALLMENT_REFUND, MAINTENANCE_FEE, CANCELLATION_CHARGE

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description; // Optional text explaining the entry

    @CreationTimestamp
    private LocalDateTime createdAt;
}

