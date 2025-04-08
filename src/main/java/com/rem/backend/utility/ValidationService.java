package com.rem.backend.utility;

import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;

import java.util.Comparator;
import java.util.List;
import java.util.Set;


public class ValidationService {


    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    public static <T> void validate(T object, String variableName) {

        if (object == null) {
            throw new IllegalArgumentException(variableName + " must not be null.");
        }else if(object == ""){
            throw new IllegalArgumentException(variableName + " must not be empty.");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(object);

        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            for (ConstraintViolation<T> violation : violations) {
                errorMessage.append(variableName)
                        .append(" ")
                        .append(violation.getMessage()) // Appends only the error message
                        .append("\n");
            }
            throw new IllegalArgumentException(errorMessage.toString().trim());
        }
    }


    public static void validatePaymentSchedule(PaymentSchedule schedule) {
        if (schedule.getDurationInMonths() <= 0) {
            throw new IllegalArgumentException("Duration in months must be greater than 0.");
        }

        if (schedule.getActualAmount() <= 0) {
            throw new IllegalArgumentException("Actual amount must be greater than 0.");
        }

        if (schedule.getTotalAmount() <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than 0.");
        }

        if (schedule.getPaymentScheduleType() == null) {
            throw new IllegalArgumentException("Schedule type must be provided.");
        }

        if (schedule.getUnit() == null) {
            throw new IllegalArgumentException("Unit must be associated.");
        }

        if (schedule.getCreatedBy() == null || schedule.getCreatedBy().isBlank()) {
            throw new IllegalArgumentException("CreatedBy must be provided.");
        }

        if (schedule.getUpdatedBy() == null || schedule.getUpdatedBy().isBlank()) {
            throw new IllegalArgumentException("UpdatedBy must be provided.");
        }
    }

    public static void validateMonthWisePayments(List<MonthWisePayment> payments, int durationInMonths) {
        if (payments == null || payments.isEmpty()) {
            throw new IllegalArgumentException("Month-wise payments list cannot be null or empty.");
        }

        payments.sort(Comparator.comparingInt(MonthWisePayment::getFromMonth));

        int expectedStart = 1;

        for (MonthWisePayment payment : payments) {
            int from = payment.getFromMonth();
            int to = payment.getToMonth();

            if (from >= to) {
                throw new IllegalArgumentException("Invalid range: fromMonth must be less than toMonth.");
            }

            if (from != expectedStart) {
                throw new IllegalArgumentException("Month ranges must start from 1 and be continuous without gaps. Expected fromMonth: " + expectedStart);
            }

            if (to > durationInMonths) {
                throw new IllegalArgumentException("toMonth cannot be greater than durationInMonths: " + durationInMonths);
            }

            expectedStart = to;
        }

        if (expectedStart != durationInMonths) {
            throw new IllegalArgumentException("Final toMonth must match durationInMonths. Last toMonth was: " + expectedStart);
        }
    }


    public static void validateBooking(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null.");
        }

        if (booking.getCustomer() == null) {
            throw new IllegalArgumentException("Customer must be provided.");
        }

        if (booking.getUnit() == null) {
            throw new IllegalArgumentException("Unit must be provided.");
        }

        PaymentSchedule paymentSchedule = booking.getPaymentSchedule();

        if (paymentSchedule != null) {
            // Basic checks before deeper validation
            if (paymentSchedule.getDurationInMonths() <= 0) {
                throw new IllegalArgumentException("Duration in months must be greater than 0.");
            }

            if (paymentSchedule.getActualAmount() <= 0) {
                throw new IllegalArgumentException("Actual amount must be greater than 0.");
            }

            if (paymentSchedule.getTotalAmount() <= 0) {
                throw new IllegalArgumentException("Total amount must be greater than 0.");
            }

            if (paymentSchedule.getCreatedBy() == null || paymentSchedule.getCreatedBy().isBlank()) {
                throw new IllegalArgumentException("CreatedBy must be provided in PaymentSchedule.");
            }

            if (paymentSchedule.getUpdatedBy() == null || paymentSchedule.getUpdatedBy().isBlank()) {
                throw new IllegalArgumentException("UpdatedBy must be provided in PaymentSchedule.");
            }
        }
    }


}


