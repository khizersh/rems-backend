package com.rem.backend.service;

import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.entity.project.Floor;
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.enums.PaymentPlanType;
import com.rem.backend.enums.PaymentScheduleType;
import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.enums.PaymentType;
import com.rem.backend.repository.*;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.repository.UserRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.*;

import static com.rem.backend.utility.Utility.*;
import static com.rem.backend.utility.ValidationService.*;

@Transactional
@AllArgsConstructor
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ProjectRepo projectRepo;
    private final FloorRepo floorRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerRepo customerRepo;
    private final CustomerService customerService;
    private final UnitRepo unitRepo;
    private final PaymentSchedulerService paymentSchedulerService;
    private final PaymentScheduleRepository paymentScheduleRepo;
    private final CustomerPaymentRepo customerPaymentRepo;
    private final UserRepo userRepo;


    @Transactional
    public Map<String, Object> createBooking(Booking booking, String loggedInUser) {

        try {
            booking.setCreatedBy(loggedInUser);
            booking.setUpdatedBy(loggedInUser);
            validateBooking(booking);


            if (bookingRepository.existsByUnit_Id(booking.getUnitId()))
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "This unit is already booked!");

            Customer customer = null;
            if (booking.getCustomerId() != null) {
                Optional<Customer> customerOptional = customerRepo.findById(booking.getCustomerId());
                if (customerOptional.isPresent()) {
                    customer = customerOptional.get();
                }
            } else {
                Map<String, Object> createCustomer = customerService.createCustomer(booking.getCustomer(), loggedInUser);
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
            unit.setBooked(true);


            Unit savedUnit = unitRepo.save(unit);


            PaymentSchedule paymentSchedule = booking.getPaymentSchedule();
            paymentSchedule.setCreatedBy(loggedInUser);
            paymentSchedule.setUpdatedBy(loggedInUser);
            paymentSchedule.setUnit(booking.getUnit());
            paymentSchedule.setPaymentScheduleType(PaymentScheduleType.CUSTOMER);

            Map<String, Object> createPaymentScheduler = paymentSchedulerService.createSchedule(paymentSchedule, paymentSchedule.getPaymentPlanType());
            if (createPaymentScheduler != null) {
                PaymentSchedule savedSchedule = null;
                if (!createPaymentScheduler.get(RESPONSE_CODE).equals(Responses.SUCCESS.getResponseCode())) {
                    return createPaymentScheduler;
                }
                createPaymentScheduler.get(DATA);
            }


            Optional<Floor> optionalFloor = floorRepo.findById(unit.getFloorId());

            if (optionalFloor.isPresent()) {
                booking.setProjectId(optionalFloor.get().getProjectId());
            }

            booking.setUnitSerial(unit.getSerialNo());
            booking.setCreatedBy(loggedInUser);
            booking.setUpdatedBy(loggedInUser);
            booking.setFloorId(unit.getFloorId());
            Booking bookingSaved = bookingRepository.save(booking);
            createCustomerAccount(bookingSaved, loggedInUser, paymentSchedule.getPaymentPlanType());

            return ResponseMapper.buildResponse(Responses.SUCCESS, bookingSaved);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    public void createCustomerAccount(Booking booking, String loggedInUser, PaymentPlanType paymentPlanType) {
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


        double totalAmount = Math.ceil(schedule.getActualAmount() + schedule.getMiscellaneousAmount());

        if (paymentPlanType.equals(PaymentPlanType.ONE_TIME_PAYMENT)) {
            schedule.setDownPayment(totalAmount);
            schedule.setDurationInMonths(0);
        }

        double monthlySum = monthlyPaymentSum(schedule);
        double collectedAmount = Math.ceil(schedule.getDownPayment() +
                schedule.getOnPossessionPayment() + monthlySum);


        if (totalAmount != collectedAmount) {
            throw new IllegalArgumentException("Amounts not matched!");
        }

        CustomerAccount customerAccountSaved = customerAccountRepo.save(account);

        int serialNoStart = 1;


        if (schedule.getDownPayment() > 0) {
//            serialNoStart  = 2;


//        insert entry in customer_payment for a down payment
            CustomerPayment customerPayment = new CustomerPayment();
            customerPayment.setSerialNo(0);
            customerPayment.setAmount(schedule.getDownPayment());
            customerPayment.setReceivedAmount(0);
            customerPayment.setRemainingAmount(schedule.getDownPayment());
            customerPayment.setPaymentType(PaymentType.CASH);
            customerPayment.setPaymentStatus(PaymentStatus.UNPAID);
            customerPayment.setCreatedBy(booking.getCreatedBy());
            customerPayment.setUpdatedBy(booking.getUpdatedBy());
            customerPayment.setCreatedDate(LocalDateTime.now());
            customerPayment.setUpdatedDate(LocalDateTime.now());
            customerPayment.setCustomerAccountId(customerAccountSaved.getId());

            customerPaymentRepo.save(customerPayment);
        }


        for (int i = 0; i < schedule.getDurationInMonths(); i++) {
            int serialNo = i + serialNoStart;
            Optional<MonthWisePayment> monthWisePaymentOptional = schedule.getMonthWisePaymentList().stream()
                    .filter(payment -> serialNo >= payment.getFromMonth() && serialNo <= payment.getToMonth())
                    .findFirst();

            if (monthWisePaymentOptional.isEmpty()) {
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
            customerPayment.setRemainingAmount(amount);
            customerPayment.setPaymentType(PaymentType.CASH);
            customerPayment.setPaymentStatus(PaymentStatus.UNPAID);
            customerPayment.setCreatedBy(booking.getCreatedBy());
            customerPayment.setUpdatedBy(booking.getUpdatedBy());
            customerPayment.setCreatedDate(LocalDateTime.now());
            customerPayment.setUpdatedDate(LocalDateTime.now());
            customerPayment.setCustomerAccountId(customerAccountSaved.getId());

            customerPaymentRepo.save(customerPayment);

        }


    }

    public double monthlyPaymentSum(PaymentSchedule schedule) {
        double sum = 0.0;
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
        return sum;
    }


    public Map<String, Object> getBookingsByIds(long id, String filteredBy, Pageable pageable) {
        try {
            Page<Booking> bookings = null;
            ValidationService.validate(id, filteredBy);

            // Fetch bookings based on filter
            switch (filteredBy) {
                case "organization":
                    bookings = bookingRepository.findByOrganizationId(id, pageable);
                    break;
                case "project":
                    bookings = bookingRepository.findByProjectId(id, pageable);
                    break;
                case "floor":
                    bookings = bookingRepository.findByFloorId(id, pageable);
                    break;
                case "unit":
                    bookings = bookingRepository.findByUnitId(id, pageable);
                    break;
                default:
                    bookings = bookingRepository.findByOrganizationId(id, pageable);
                    break;
            }

            // Populate transient fields for API response

            bookings.getContent().forEach(booking -> {
                Unit unit = booking.getUnit();
                booking.setCustomerName(booking.getCustomer().getName());
                booking.setCustomerId(booking.getCustomer().getCustomerId());
                booking.setUnitSerial(unit.getSerialNo());

                Optional<PaymentSchedule> paymentScheduleOptional = paymentScheduleRepo.findByUnitIdAndPaymentScheduleType(unit.getId(), PaymentScheduleType.CUSTOMER);

                if (paymentScheduleOptional.isPresent()) {
                    booking.setTotalAmount(paymentScheduleOptional.get().getTotalAmount());
                }

                Optional<Floor> optionalFloor = floorRepo.findById(unit.getFloorId());
                if (optionalFloor.isPresent()) {
                    String projectName = projectRepo.findProjectNameById(optionalFloor.get().getProjectId());
                    booking.setProject(projectName);
                    booking.setFloorNo(optionalFloor.get().getFloor());
                }


            });

            return ResponseMapper.buildResponse(Responses.SUCCESS, bookings);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getDetailsById(long bookingId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));


            Customer customer = booking.getCustomer();
            Optional<User> userOptional = userRepo.findById(customer.getUserId());
            Map<String, Object> customerMap = new HashMap<>();
            if (customer != null) {
                customerMap.put("customerId", customer.getCustomerId());
                customerMap.put("userId", customer.getUserId());
                customerMap.put("name", customer.getName());
                customerMap.put("country", customer.getCountry());
                customerMap.put("city", customer.getCity());
                customerMap.put("address", customer.getAddress());
                customerMap.put("nationalId", customer.getNationalId());
                customerMap.put("nextOFKinName", customer.getNextOFKinName());
                customerMap.put("guardianName", customer.getGuardianName());
                customerMap.put("contactNo", customer.getContactNo());
                customerMap.put("nextOFKinNationalId", customer.getNextOFKinNationalId());
                customerMap.put("relationShipWithKin", customer.getRelationShipWithKin());
                customerMap.put("organizationId", customer.getOrganizationId());
                customerMap.put("projectId", customer.getProjectId());
                customerMap.put("floorId", customer.getFloorId());
                customerMap.put("unitId", customer.getUnitId());
                customerMap.put("projectName", customer.getProjectName());
                customerMap.put("floorNo", customer.getFloorNo());
                customerMap.put("unitSerialNo", customer.getUnitSerialNo());
                customerMap.put("createdBy", customer.getCreatedBy());
                customerMap.put("updatedBy", customer.getUpdatedBy());
                customerMap.put("createdDate", customer.getCreatedDate());
                customerMap.put("updatedDate", customer.getUpdatedDate());
                customerMap.put("email", userOptional.get().getEmail());
                customerMap.put("username", customer.getUsername());
                customerMap.put("age", customer.getAge());
            }


            Unit unit = booking.getUnit();
            Map<String, Object> unitMap = new HashMap<>();
            if (unit != null) {
                unitMap.put("id", unit.getId());
                unitMap.put("serialNo", unit.getSerialNo());
                unitMap.put("squareFoot", unit.getSquareFoot());
                unitMap.put("roomCount", unit.getRoomCount());
                unitMap.put("bathroomCount", unit.getBathroomCount());
                unitMap.put("amount", unit.getAmount());
                unitMap.put("floorId", unit.getFloorId());
                unitMap.put("additionalAmount", unit.getAdditionalAmount());
                unitMap.put("unitType", unit.getUnitType());
                unitMap.put("isBooked", unit.isBooked());
                unitMap.put("paymentPlanType", unit.getPaymentPlanType());
                unitMap.put("createdBy", unit.getCreatedBy());
                unitMap.put("updatedBy", unit.getUpdatedBy());
                unitMap.put("createdDate", unit.getCreatedDate());
                unitMap.put("updatedDate", unit.getUpdatedDate());
                unitMap.put("projectName", unit.getProjectName());
                unitMap.put("floorNo", unit.getFloorNo());
            }

            // Final response
            response.put("customer", customerMap);
            response.put("unit", unitMap);

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


}
