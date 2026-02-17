package com.rem.backend.customermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerSummaryDTO {
    private String customerName;
    private String nationalId;
    private String contactNo;
    private String email;
    private int totalBookings;
    private int totalUnitsBooked;
    private double totalAmountPayable;
    private double totalAmountPaid;
    private double totalRemainingAmount;
    private double overdueAmount;
}
