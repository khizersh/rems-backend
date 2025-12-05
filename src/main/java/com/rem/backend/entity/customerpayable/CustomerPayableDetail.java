package com.rem.backend.entity.customerpayable;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_payable_details")
@Getter
@Setter
public class CustomerPayableDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_payable_id", nullable = false)
    private CustomerPayable customerPayable;

    @Column(nullable = false)
    private String paymentType;

    @Column(nullable = false)
    private BigDecimal amount;

    private String chequeNo;

    private LocalDateTime chequeDate;

    private String createdBy;
    private String updatedBy;

    @CreationTimestamp
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;
}


