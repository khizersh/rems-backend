package com.rem.backend.service;

import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.enums.PaymentScheduleType;
import com.rem.backend.repository.BookingRepository;
import com.rem.backend.repository.PaymentScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import static com.rem.backend.utility.ValidationService.*;

@Transactional
@AllArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;


    @Transactional
    public Booking createBooking(Booking booking) {
        validateBooking(booking);

        PaymentSchedule paymentSchedule = booking.getPaymentSchedule();

        if (paymentSchedule != null) {
            // Set type to CUSTOMER and associate with unit
            paymentSchedule.setUnit(booking.getUnit());
            paymentSchedule.setPaymentScheduleType(PaymentScheduleType.CUSTOMER);

            validatePaymentSchedule(paymentSchedule);
            validateMonthWisePayments(paymentSchedule.getMonthWisePaymentList(), paymentSchedule.getDurationInMonths());

            for (MonthWisePayment mwp : paymentSchedule.getMonthWisePaymentList()) {
                mwp.setPaymentSchedule(paymentSchedule);
            }



            PaymentSchedule savedSchedule = paymentScheduleRepository.save(paymentSchedule);
            booking.setPaymentSchedule(savedSchedule); // Even if @Transient, this keeps runtime data aligned
        }

        return bookingRepository.save(booking);
    }

}
