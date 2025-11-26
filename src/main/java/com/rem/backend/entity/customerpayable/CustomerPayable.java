package com.rem.backend.entity.customerpayable;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customer_payable")
public class CustomerPayable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;         // FK → booking table

    @Column(nullable = false)
    private Long customerId;        // FK → customer table

    @Column(nullable = false)
    private Long unitId;            // FK → unit table

    @Column(nullable = false)
    private BigDecimal totalPayable;    // Sum of all details (refunds + deductions)

    @Column(nullable = false)
    private BigDecimal totalRefund;     // Optional → refundable amount to customer

    @Column(nullable = false)
    private BigDecimal totalDeductions; // Cancellation charges etc.

    @Column(nullable = false)
    private String reason;              // e.g. "Customer Requested", "Default", etc.

    @Column(nullable = false)
    private String status;              // PENDING, PROCESSED, CANCELLED

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "customerPayable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CustomerPayableDetail> details;
}
