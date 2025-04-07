package com.rem.backend.entity.paymentschedule;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "month_wise_payment")
@Data
public class MonthWisePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private int fromMonth;

    @Column(nullable = false)
    private int toMonth;

    @Column(nullable = false)
    private double amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_schedule_id", nullable = false)
    private PaymentSchedule paymentSchedule;
}