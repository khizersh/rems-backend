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
    @JoinColumn(name = "customer_id_mapping")
    @JsonIgnore
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id_mapping")
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
    private Long organizationId;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long floorId;


    @Column(nullable = false)
    private Long unitId;

    @Column(nullable = false)
    private Long customerId;


    @Transient
    private String projectName;

    @Transient
    private String floorNo;

    @Transient
    private String unitSerialNo;

    @Transient
    private double totalAmount;

    @Transient
    private PaymentSchedule paymentSchedule;

    @Transient
    private String project;

    @Transient
    private String customerName;

    private String unitSerial;



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
