package com.rem.backend.customermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentChartDTO {
    private int month;
    private int year;
    private String monthName;
    private double totalPaidAmount;
    private double totalDueAmount;
    private double cumulativeRemaining;
}
