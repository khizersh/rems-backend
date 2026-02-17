package com.rem.backend.customermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentModeDistributionDTO {
    private String paymentMode;
    private double totalAmount;
    private int transactionCount;
}
