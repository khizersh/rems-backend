package com.rem.backend.entity.project;

import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.enums.PaymentPlanType;
import com.rem.backend.enums.UnitType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name = "unit")
@Data
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String serialNo;

    @Column(nullable = false)
    private int squareFoot;

    @Column(nullable = false)
    private int roomCount = 0;

    @Column(nullable = false)
    private int bathroomCount = 0;

    @Column(nullable = true)
    private double amount = 0.0;

    @Column(nullable = false)
    private long floorId;

    @Column(nullable = true)
    private double additionalAmount = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitType unitType;

    @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isBooked;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentPlanType paymentPlanType;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;


    @Transient
    private String projectName;

    @Transient
    private int floorNo;

    @Transient
    private PaymentSchedule paymentSchedule;

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
