package com.rem.backend.entity.customerpayable;

import com.rem.backend.enums.FeeType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_payable_fee_details")
@Data
public class CustomerPayableFeeDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_payable_id", nullable = false)
    private CustomerPayable customerPayable;

    @Column(nullable = false)
    private FeeType type; // FIXED | PERCENTILE

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private double inputValue; // 10 (%) or 5000

    @Column(nullable = false)
    private double calculatedAmount;

    @Column(nullable = false)
    private boolean deduction; // true = deduction, false = refund

    private String createdBy;

    private String updatedBy;

    @CreationTimestamp
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;
}

