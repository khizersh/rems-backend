package com.rem.backend.entity.expense;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name = "expense_detail")
@Data
public class ExpenseDetail {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private Long expenseId;


    @Column(nullable = false)
    private Long organizationAccountId;

    @Column(nullable = false)
    private double amountPaid;


    @Column(nullable = false)
    private String organizationAccountTitle;

    @Column(nullable = false)
    private String expenseTitle;

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
