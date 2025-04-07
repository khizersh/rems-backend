package com.rem.backend.entity.paymentschedule;


import com.rem.backend.entity.project.Apartment;
import com.rem.backend.enums.ScheduleType;
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

    @Column(nullable = true)
    private double miscellaneousAmount = 0.0;

    @Column(nullable = false)
    private double totalAmount;

    @Column(nullable = true)
    private double downPayment = 0.0;

    @Column(nullable = true)
    private double quarterlyPayment = 0.0;

    @Column(nullable = true)
    private double halfYearlyPayment = 0.0;

    @Column(nullable = true)
    private double yearlyPayment = 0.0;

    @Column(nullable = true)
    private double onPossessionPayment = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    @OneToMany(mappedBy = "paymentSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonthWisePayment> monthWisePaymentList;

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
