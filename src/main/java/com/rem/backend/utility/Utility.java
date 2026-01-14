package com.rem.backend.utility;

import com.rem.backend.entity.expense.Expense;
import com.rem.backend.entity.paymentschedule.MonthSpecificPayment;
import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.enums.PaymentPlanType;
import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.dto.booking.BookingCancellationRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

public class Utility {

    public static final String RESPONSE_CODE = "responseCode";
    public static final String RESPONSE_MESSAGE = "responseMessage";
    public static final String DATA = "data";
    public static final String COMPANY_NAME = "COMPANY_NAME";
    public static final long ADMIN_ROLE_ID = 1;
    public static final long USER_ROLE_ID = 2;


    public static LocalDateTime getDateInLastDays(int days) {
        return LocalDateTime.now().minusDays(days);
    }

    public static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            // JPEG family
            "image/jpeg",
            "image/jpg",
            "image/pjpeg",

            // PNG
            "image/png",

            // Web formats
            "image/webp",
            "image/avif",

            // GIF & BMP
            "image/gif",
            "image/bmp",
            "image/x-ms-bmp",

            // TIFF
            "image/tiff",

            // JPEG 2000
            "image/jp2",
            "image/jpeg2000",

            // Photoshop
            "image/vnd.adobe.photoshop"
    );

    public static LocalDateTime getStartOfDay(String input) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
        LocalDate date = LocalDate.parse(input, formatter);
        return date.atStartOfDay(); // 00:00:00
    }

    public static LocalDateTime getEndOfDay(String input) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
        LocalDate date = LocalDate.parse(input, formatter);
        return date.atTime(LocalTime.MAX); // 23:59:59.999999999
    }


    public static double monthlyPaymentSum(PaymentSchedule schedule) {
        double sum = 0.0;

        if (schedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_SPECIFIC)) {
            for (MonthSpecificPayment payment : schedule.getMonthSpecificPaymentList()){
                sum += payment.getAmount();
            }
        } else if (schedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_RANGE)) {
            for (int i = 0; i < schedule.getDurationInMonths(); i++) {
                int serialNo = i + 1;
                Optional<MonthWisePayment> monthWisePaymentOptional = schedule.getMonthWisePaymentList().stream()
                        .filter(payment -> serialNo >= payment.getFromMonth() && serialNo <= payment.getToMonth())
                        .findFirst();

                if (monthWisePaymentOptional.isEmpty()) {
                    throw new IllegalArgumentException("Invalid Month wise payment!");
                }
                double amount = monthWisePaymentOptional.get().getAmount();

                // Add special amounts based on serialNo
                if (serialNo % 3 == 0 && schedule.getQuarterlyPayment() != 0) {
                    amount += schedule.getQuarterlyPayment();
                }

                if (serialNo % 6 == 0 && schedule.getHalfYearlyPayment() != 0) {
                    amount += schedule.getHalfYearlyPayment();
                }

                if (serialNo % 12 == 0 && schedule.getYearlyPayment() != 0) {
                    amount += schedule.getYearlyPayment();
                }
                sum += amount;
            }
        }
        return sum;
    }


    public static PaymentStatus getPaymentStatus(Expense expense) {

        if (expense.getAmountPaid() == expense.getTotalAmount())
            return PaymentStatus.PAID;

        if (expense.getAmountPaid() == 0)
            return PaymentStatus.UNPAID;

        if (expense.getCreditAmount() > 0)
            return PaymentStatus.PENDING;

        return PaymentStatus.PENDING;
    }


    public static double calculateFee(double deposited, BookingCancellationRequest.CustomerPayableFeesDto fee) {
        return switch (fee.getType()) {
            case PERCENTILE -> deposited * (fee.getValue() / 100.0);
            case FIXED -> fee.getValue();
        };
    }



}
