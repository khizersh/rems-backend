package com.rem.backend.customermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountStatusDTO {
    private long accountId;
    private String projectName;
    private String unitSerial;
    private String unitType;
    private double totalAmount;
    private double totalPaidAmount;
    private double totalBalanceAmount;
    private String status;
    private int durationInMonths;
}
