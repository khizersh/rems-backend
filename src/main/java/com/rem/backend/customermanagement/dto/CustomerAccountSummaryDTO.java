package com.rem.backend.customermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAccountSummaryDTO {
    private long customerId;
    private String customerName;
    private String customerContact;
    private int totalAccounts;
    private int activeAccounts;
    private int closedAccounts;
    private double totalBookingAmount;
    private double totalPaidAmount;
    private double totalBalanceAmount;
    private double totalOverdueAmount;
    private int totalPaymentsCount;
    private double averagePaymentAmount;
}
