package com.rem.backend.entity.paymentschedule;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.enums.PaymentPlanType;
import com.rem.backend.enums.PaymentScheduleType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "payment_schedule")
@Data
public class PaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private int durationInMonths;

    @Column(nullable = false)
    private double actualAmount;

    @Column(nullable = false)
    private double miscellaneousAmount = 0.0;


    @Column(nullable = false )
    private double developmentAmount = 0.0;

    @Column(nullable = false)
    private double totalAmount;

    @Column(nullable = false)
    private double downPayment = 0.0;

    @Column(nullable = false)
    private double quarterlyPayment = 0.0;

    @Column(nullable = false)
    private double halfYearlyPayment = 0.0;

    @Column(nullable = false)
    private double yearlyPayment = 0.0;

    @Column(nullable = false)
    private double onPossessionPayment = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentScheduleType paymentScheduleType;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentPlanType paymentPlanType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    @JsonIgnore
    private Unit unit;

    @Transient
    private List<MonthWisePayment> monthWisePaymentList;


    @Transient
    private List<MonthSpecificPayment> monthSpecificPaymentList;

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
