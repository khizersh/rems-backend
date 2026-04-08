package com.rem.backend.entity.expense;


import com.rem.backend.enums.ExpenseType;
import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.enums.PaymentType;
import com.rem.backend.enums.PdcStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense")
@Data
public class Expense {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;


    private double amountPaid;
    private double creditAmount;
    private double totalAmount;
    private Long vendorAccountId;
    private Long organizationAccountId;

    @Column(nullable = true)
    private Long expenseTypeId;

    private Long organizationId;

    @Column(nullable = true)
    private Long projectId;


    private Long unitId; //optional
    private String projectName;
    private String orgAccountTitle;
    private String vendorName;
    private String expenseTitle;
    private long expenseCOAId; //only present when expense is non construction
    private String comments = "Miscellaneous Expense";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseType expenseType;

    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    // -- PDC (Post-Dated Cheque) Fields --
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private PdcStatus pdcStatus;

    @Column(nullable = true)
    private String chequeNumber;

    @Column(nullable = true)
    private LocalDate chequeDate;

    @Column(nullable = true)
    private String bankName;
    // ────────────────────────────────────────────────────────────────

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @Transient
    private PaymentType paymentType;

    @Transient
    private String paymentDocNo;

    @Transient
    private String expenseAccountName;

    @Transient
    private LocalDateTime paymentDocDate;



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
