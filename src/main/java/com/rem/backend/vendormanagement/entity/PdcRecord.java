package com.rem.backend.vendormanagement.entity;

import com.rem.backend.enums.ExpenseType;
import com.rem.backend.enums.PdcStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Post-Dated Cheque (PDC) Record
 * Represents a future payment commitment via cheque.
 * No immediate financial impact - expense is created only when PDC is cleared.
 */
@Entity
@Table(name = "pdc_record")
@Data
public class PdcRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private Long vendorAccountId;

    @Column(nullable = true)
    private Long projectId;

    @Column(nullable = true)
    private Long unitId;

    @Column(nullable = true)
    private Long expenseTypeId;

    @Column(nullable = false)
    private Long organizationAccountId; // Account that will be debited when cleared

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private double paidAmount = 0.0; // Track partial payments

    @Column(nullable = false)
    private String chequeNumber;

    @Column(nullable = false)
    private LocalDate chequeDate;

    @Column(nullable = true)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PdcStatus status = PdcStatus.PENDING;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private ExpenseType expenseType;

    @Column(nullable = true)
    private Long expenseId; // Linked expense ID after clearance

    @Column(nullable = false)
    private String title;

    @Column(nullable = true)
    private String comments;

    // Transient fields for response enrichment
    @Transient
    private String vendorName;

    @Transient
    private String projectName;

    @Transient
    private String orgAccountTitle;

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
        this.updatedDate = LocalDateTime.now();
    }
}
