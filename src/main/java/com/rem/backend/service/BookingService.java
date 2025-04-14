package com.rem.backend.service;

import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.entity.project.Project;
import com.rem.backend.enums.PaymentScheduleType;
import com.rem.backend.enums.PaymentType;
import com.rem.backend.repository.BookingRepository;
import com.rem.backend.repository.CustomerAccountRepo;
import com.rem.backend.repository.PaymentScheduleRepository;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.rem.backend.utility.ValidationService.*;

@Transactional
@AllArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final ProjectRepo projectRepo;
    private final CustomerAccountRepo customerAccountRepo;


    @Transactional
    public Map<String, Object> createBooking(Booking booking, String loggedInUser) {

        try {
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
                booking.setPaymentSchedule(savedSchedule);
            }

            booking.setCreatedBy(loggedInUser);
            booking.setUpdatedBy(loggedInUser);
            Booking bookingSaved = bookingRepository.save(booking);
            createCustomerAccount(bookingSaved , loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, bookingSaved);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        }

    }


    public void createCustomerAccount(Booking booking, String loggedInUser) {
        if (booking == null || booking.getPaymentSchedule() == null) {
            throw new IllegalArgumentException("Booking or its PaymentSchedule cannot be null");
        }

        PaymentSchedule schedule = booking.getPaymentSchedule();

        CustomerAccount account = new CustomerAccount();
        account.setCustomer(booking.getCustomer());
        account.setUnit(booking.getUnit());

        if (booking.getUnit() != null) {
            Optional<Project> projectOptional = projectRepo.findByProjectIdAndIsActiveTrue(booking.getCustomer().getProjectId());
            if (projectOptional.isPresent())
                account.setProject(projectOptional.get());
        }

        account.setDurationInMonths(schedule.getDurationInMonths());
        account.setActualAmount(schedule.getActualAmount());
        account.setMiscellaneousAmount(schedule.getMiscellaneousAmount());
        account.setDownPayment(schedule.getDownPayment());
        account.setTotalAmount(schedule.getTotalAmount());
        account.setQuarterlyPayment(schedule.getQuarterlyPayment());
        account.setHalfYearly(schedule.getHalfYearlyPayment());
        account.setOnPosessionAmount(schedule.getOnPossessionPayment());

        account.setCreatedBy(booking.getCreatedBy());
        account.setUpdatedBy(booking.getUpdatedBy());
        account.setCreatedDate(LocalDateTime.now());
        account.setUpdatedDate(LocalDateTime.now());
        account.setCreatedBy(loggedInUser);
        account.setUpdatedBy(loggedInUser);

        double monthlyAmount = (schedule.getTotalAmount() - schedule.getDownPayment()) / schedule.getDurationInMonths();
        List<CustomerPayment> paymentList = new ArrayList<>();

        for (int i = 0; i < schedule.getDurationInMonths(); i++) {
            int serialNo = i + 1;
            double amount = monthlyAmount;

            // Add special amounts based on serialNo
            if (serialNo == 4 && schedule.getQuarterlyPayment() != 0) {
                amount += schedule.getQuarterlyPayment();
            }

            if (serialNo == 6 && schedule.getHalfYearlyPayment() != 0) {
                amount += schedule.getHalfYearlyPayment();
            }

            if (serialNo == 12 && schedule.getYearlyPayment() != 0) {
                amount += schedule.getYearlyPayment();
            }

            CustomerPayment payment = new CustomerPayment();
            payment.setSerialNo(serialNo);
            payment.setAmount(amount);
            payment.setReceivedAmount(0);
            payment.setPaymentType(PaymentType.CASH); // Adjust if needed
            payment.setCreatedBy(booking.getCreatedBy());
            payment.setUpdatedBy(booking.getUpdatedBy());
            payment.setCreatedDate(LocalDateTime.now());
            payment.setUpdatedDate(LocalDateTime.now());
            payment.setCustomerAccount(account);

            paymentList.add(payment);
        }

        account.setCustomerPayments(paymentList);

        customerAccountRepo.save(account);
    }

}
