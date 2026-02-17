package com.rem.backend.customermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentPaymentDTO {
    private long paymentId;
    private long accountId;
    private String projectName;
    private String unitSerial;
    private double totalPaymentAmount;
    private double receivedAmount;
    private LocalDateTime paidDate;
    private String paymentStatus;
    private List<PaymentDetailDTO> paymentDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentDetailDTO {
        private String paymentType;
        private double amount;
        private String chequeNo;
        private LocalDateTime chequeDate;
    }
}
