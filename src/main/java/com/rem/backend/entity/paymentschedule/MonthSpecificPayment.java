package com.rem.backend.entity.paymentschedule;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "month_specific_payment")
@Data
public class MonthSpecificPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private Long paymentScheduleId;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String month;

    @Column(nullable = false)
    private String year;



}
