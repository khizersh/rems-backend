package com.rem.backend.entity.customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.project.Unit;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "customer_account"
//        uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "unit_id" , "ia_active"})
)
@Data
public class CustomerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private int durationInMonths;

    @Column(nullable = false)
    private double actualAmount;

    @Column(nullable = false)
    private double miscellaneousAmount;

    @Column(nullable = false)
    private double developmentAmount;

    @Column(nullable = false)
    private double downPayment;

    @Column(nullable = false)
    private double totalAmount;

    @Column(nullable = true)
    private double quarterlyPayment;

    @Column(nullable = true)
    private double halfYearly;

    @Column(nullable = true)
    private double onPosessionAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Unit unit;

    @Column(nullable = false)
    private Double totalPaidAmount = 0.0;

    @Column(nullable = false)
    private Double totalBalanceAmount = 0.0;

    @Transient
    private List<CustomerPayment> customerPayments;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean isActive = true;

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
    }
}