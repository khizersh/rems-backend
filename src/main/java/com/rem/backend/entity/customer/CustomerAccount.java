package com.rem.backend.entity.customer;

import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.project.Unit;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customer_account")
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
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @OneToMany(mappedBy = "customerAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerPayment> customerPayments;

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