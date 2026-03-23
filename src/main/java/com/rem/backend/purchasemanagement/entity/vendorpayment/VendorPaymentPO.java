package com.rem.backend.purchasemanagement.entity.vendorpayment;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_payment_po")
@Data
public class VendorPaymentPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;

    private Long projectId;

    private Long vendorId;

    private Long invoiceId;

    private Double amount;

    private String paymentMode; // CASH, CHEQUE, BANK_TRANSFER

    private String referenceNumber;

    private LocalDate paymentDate;

    private String remarks;

    // track which organization account was used for this payment (nullable)
    private Long organizationAccountId;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

}
