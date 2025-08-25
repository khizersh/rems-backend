package com.rem.backend.entity.booking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.project.Unit;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name = "booking")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    @JsonIgnore
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id")
    @JsonIgnore
    private Unit unit;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @Column(nullable = false)
    private long organizationId;

    @Column(nullable = false)
    private long projectId;

    @Column(nullable = false)
    private long floorId;


    @Transient
    private double totalAmount;


    @Transient
    private PaymentSchedule paymentSchedule;

    @Transient
    private String project;

    @Transient
    private String customerName;

    private String unitSerial;

    @Transient
    private int floorNo;

    @Transient
    private Long unitId;

    @Transient
    private Long customerId;


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
