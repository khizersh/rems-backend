package com.rem.backend.customermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerLedgerDTO {
    private long id;
    private String transactionType; // PAYMENT_IN, REFUND_OUT, ADJUSTMENT
    private String referenceType; // PAYMENT, REFUND, ADJUSTMENT
    private long referenceId;
    private String description;
    private double debitAmount;
    private double creditAmount;
    private double runningBalance;
    private LocalDateTime transactionDate;
    private String customerName;
    private String unitSerial;
    private String projectName;
    private String paymentMode;
    private String chequeNo;
    private LocalDateTime chequeDate;
    private String createdBy;
}
