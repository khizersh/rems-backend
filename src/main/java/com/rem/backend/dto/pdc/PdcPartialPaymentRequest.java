package com.rem.backend.dto.pdc;

import com.rem.backend.enums.PaymentType;
import lombok.Data;

@Data
public class PdcPartialPaymentRequest {
    private Long pdcId;
    private double amount;
    private Long organizationAccountId;
    private PaymentType paymentType; // CASH, BANK_TRANSFER, etc.
    private String paymentDocNo; // Reference number for cash/bank transfer
    private String comments;
}
