package com.rem.backend.customermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAccountDTO {
    private long id;
    private long customerId;
    private String customerName;
    private String customerContact;
    private String customerEmail;
    private long projectId;
    private String projectName;
    private long unitId;
    private String unitSerial;
    private String unitType;
    private int durationInMonths;
    private double actualAmount;
    private double miscellaneousAmount;
    private double developmentAmount;
    private double downPayment;
    private double totalAmount;
    private double quarterlyPayment;
    private double halfYearly;
    private double onPossessionAmount;
    private double totalPaidAmount;
    private double totalBalanceAmount;
    private boolean isActive;
    private String createdBy;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // Payment summary
    private int totalPaymentsCount;
    private double lastPaymentAmount;
    private LocalDateTime lastPaymentDate;
    private String paymentStatus;
}
