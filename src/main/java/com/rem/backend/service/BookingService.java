package com.rem.backend.service;

import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.enums.PaymentScheduleType;
import com.rem.backend.enums.PaymentType;
import com.rem.backend.repository.*;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.rem.backend.utility.Utility.*;
import static com.rem.backend.utility.ValidationService.*;

@Transactional
@AllArgsConstructor
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ProjectRepo projectRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerRepo customerRepo;
    private final CustomerService customerService;
    private final UnitRepo unitRepo;
    private final PaymentSchedulerService paymentSchedulerService;
    private final CustomerPaymentRepo customerPaymentRepo;


    @Transactional
    public Map<String, Object> createBooking(Booking booking, String loggedInUser) {

        try {
            booking.setCreatedBy(loggedInUser);
            booking.setUpdatedBy(loggedInUser);
            validateBooking(booking);


            if(bookingRepository.existsByUnit_Id(booking.getUnitId()))
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER , "This unit is already booked!");

            Customer customer = null;
            if (booking.getCustomerId() != null) {
                Optional<Customer> customerOptional = customerRepo.findById(booking.getCustomerId());
                if (customerOptional.isPresent()) {
                    customer = customerOptional.get();
                }
            } else {
                Map<String, Object> createCustomer = customerService.createCustomer(booking.getCustomer());
                if (!createCustomer.get(RESPONSE_CODE).equals(Responses.SUCCESS.getResponseCode())) {
                    return createCustomer;
                }
                customer = (Customer) createCustomer.get(DATA);
            }
            booking.setCustomer(customer);

            Optional<Unit> unitOptional = unitRepo.findById(booking.getUnitId());
            if (!unitOptional.isPresent()) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Invalid selected unit!");
            }
            Unit unit = unitOptional.get();
            booking.setUnit(unit);


            PaymentSchedule paymentSchedule = booking.getPaymentSchedule();
            paymentSchedule.setCreatedBy(loggedInUser);
            paymentSchedule.setUpdatedBy(loggedInUser);
            paymentSchedule.setUnit(booking.getUnit());
            paymentSchedule.setPaymentScheduleType(PaymentScheduleType.CUSTOMER);

            Map<String, Object> createPaymentScheduler = paymentSchedulerService.createSchedule(paymentSchedule);
            if (createPaymentScheduler != null) {
                PaymentSchedule savedSchedule = null;
                if (!createPaymentScheduler.get(RESPONSE_CODE).equals(Responses.SUCCESS.getResponseCode())) {
                    return createPaymentScheduler;
                }
                savedSchedule = (PaymentSchedule) createPaymentScheduler.get(DATA);
                booking.setPaymentSchedule(savedSchedule);
            }

            booking.setCreatedBy(loggedInUser);
            booking.setUpdatedBy(loggedInUser);
            Booking bookingSaved = bookingRepository.save(booking);
            createCustomerAccount(bookingSaved, loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, bookingSaved);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
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


       double monthlySum = monthlyPaymentSum(schedule);
        double totalAmount = Math.ceil(schedule.getActualAmount() + schedule.getMiscellaneousAmount())  ;
        double collectedAmount = Math.ceil( schedule.getDownPayment() +
                schedule.getOnPossessionPayment() + monthlySum) ;

        if(totalAmount != collectedAmount){
            throw new IllegalArgumentException("Amounts not matched!");
        }


        CustomerAccount customerAccountSaved = customerAccountRepo.save(account);

        for (int i = 0; i < schedule.getDurationInMonths(); i++) {
            int serialNo = i + 1;
            Optional<MonthWisePayment> monthWisePaymentOptional = schedule.getMonthWisePaymentList().stream()
                    .filter(payment -> serialNo >= payment.getFromMonth() && serialNo <= payment.getToMonth())
                    .findFirst();

            if(monthWisePaymentOptional.isEmpty()){
                throw new IllegalArgumentException("Invalid Month wise customerPayment!");
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

            CustomerPayment customerPayment = new CustomerPayment();
            customerPayment.setSerialNo(serialNo);
            customerPayment.setAmount(amount);
            customerPayment.setReceivedAmount(0);
            customerPayment.setPaymentType(PaymentType.CASH); // Adjust if needed
            customerPayment.setCreatedBy(booking.getCreatedBy());
            customerPayment.setUpdatedBy(booking.getUpdatedBy());
            customerPayment.setCreatedDate(LocalDateTime.now());
            customerPayment.setUpdatedDate(LocalDateTime.now());
            customerPayment.setCustomerAccountId(customerAccountSaved.getId());

            customerPaymentRepo.save(customerPayment);

        }



    }

    public double monthlyPaymentSum(PaymentSchedule schedule){
        double sum = 0.0;
        for (int i = 0; i < schedule.getDurationInMonths(); i++) {
            int serialNo = i + 1;
            Optional<MonthWisePayment> monthWisePaymentOptional = schedule.getMonthWisePaymentList().stream()
                    .filter(payment -> serialNo >= payment.getFromMonth() && serialNo <= payment.getToMonth())
                    .findFirst();

            if(monthWisePaymentOptional.isEmpty()){
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
        return sum;
    }

}
