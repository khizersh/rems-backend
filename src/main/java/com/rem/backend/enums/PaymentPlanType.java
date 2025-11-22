package com.rem.backend.enums;

public enum PaymentPlanType {


    ONE_TIME_PAYMENT,
    INSTALLMENT_RANGE,   // Existing range-based installment (e.g., 1–10, 11–20 months)
    INSTALLMENT_SPECIFIC
}
