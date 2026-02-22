package com.rem.backend.customermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerPaymentDTO {
    private long id;
    private int serialNo;
    private long customerAccountId;
    private String customerName;
    private String unitSerial;
    private String projectName;
    private double amount;
    private double receivedAmount;
    private double remainingAmount;
    private String paymentType;
    private String paymentStatus;
    private LocalDateTime paidDate;
    private boolean isPaymentAddedToAccount;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // Payment details
    private List<CustomerPaymentDetailDTO> paymentDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomerPaymentDetailDTO {
        private long id;
        private long customerPaymentId;
        private double amount;
        private String paymentType;
        private String customerPaymentReason;
        private String chequeNo;
        private LocalDateTime chequeDate;
        private String createdBy;
        private LocalDateTime createdDate;
    }
}
