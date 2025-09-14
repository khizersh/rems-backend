package com.rem.backend.utility;

import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.enums.PaymentPlanType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
//        if (schedule.getDurationInMonths() <= 0) {
//            throw new IllegalArgumentException("Duration in months must be greater than 0.");
//        }

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

    public static void validateMonthWisePayments(List<MonthWisePayment> payments, int durationInMonths, PaymentPlanType paymentPlanType) {
        if (payments == null || payments.isEmpty() && paymentPlanType.equals(PaymentPlanType.INSTALLMENT)) {
            throw new IllegalArgumentException("Month-wise payments list cannot be null or empty.");
        }

        payments.sort(Comparator.comparingInt(MonthWisePayment::getFromMonth));

        int expectedStart = 1;

        for (MonthWisePayment payment : payments) {
            int from = payment.getFromMonth();
            int to = payment.getToMonth();

            if (from < 0) {
                throw new IllegalArgumentException("Invalid From Month");
            }
            if (to < 0) {
                throw new IllegalArgumentException("Invalid From Month");
            }


            if (from >= to) {
                throw new IllegalArgumentException("Invalid range: fromMonth must be less than toMonth.");
            }


            if (to > durationInMonths) {
                throw new IllegalArgumentException("toMonth cannot be greater than durationInMonths: " + durationInMonths);
            }

            expectedStart = to;
        }

        if (expectedStart != durationInMonths) {
            throw new IllegalArgumentException("Duration in months must match month wise payment: " + durationInMonths);
        }
    }


    public static void validateBooking(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null.");
        }

        if (booking.getCustomer() == null && booking.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer must be provided.");
        }

        if (booking.getUnit() == null && booking.getUnitId() == null) {
            throw new IllegalArgumentException("Unit must be provided.");
        }

        PaymentSchedule paymentSchedule = booking.getPaymentSchedule();

        if (paymentSchedule != null) {
            // Basic checks before deeper validation
//            if (paymentSchedule.getDurationInMonths() <= 0) {
//                throw new IllegalArgumentException("Duration in months must be greater than 0.");
//            }

            if (paymentSchedule.getActualAmount() <= 0) {
                throw new IllegalArgumentException("Actual amount must be greater than 0.");
            }

            if (paymentSchedule.getTotalAmount() <= 0) {
                throw new IllegalArgumentException("Total amount must be greater than 0.");
            }

        }
    }



    public static void validatePaymentScheduler(PaymentSchedule reqeust) {


        PaymentSchedule paymentSchedule = reqeust;

        paymentSchedule.setTotalAmount(paymentSchedule.getActualAmount() + paymentSchedule.getMiscellaneousAmount());
        if (paymentSchedule != null) {
            // Basic checks before deeper validation
//            if (paymentSchedule.getDurationInMonths() <= 0) {
//                throw new IllegalArgumentException("Duration in months must be greater than 0.");
//            }

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


    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return true;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }
}


