package com.rem.backend.entity.expense;


import com.rem.backend.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

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
    private Long expenseTypeId;
    private Long organizationId;
    private Long projectId;
    private String projectName;
    private String orgAccountTitle;
    private String  vendorName;
    private String  expenseTitle;



    @Column(nullable = false)
    private PaymentStatus paymentStatus;

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
