package com.rem.backend.service;

import com.rem.backend.dto.booking.BookingCancellationRequest;
import com.rem.backend.dto.customerpayable.CustomerPayableDto;
import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.customerpayable.CustomerPayable;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.entity.project.Floor;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.enums.PaymentScheduleType;
import com.rem.backend.repository.*;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.Utility;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.rem.backend.enums.CustomerPayableStatus.PENDING;

@Service
@RequiredArgsConstructor
public class BookingCancellationService {

    private final BookingRepository bookingRepository;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerPayableRepository customerPayableRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final ProjectRepo projectRepo;
    private final FloorRepo floorRepo;



    public Map<String, Object> getAllCanceledBooking(long orgId, Long projectId, String customerName) {

        try {
            List<Booking> list = bookingRepository
                    .findCancelledBookings(orgId, projectId, customerName);

            if(list.isEmpty()){
                return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, "No Bookings Found");
            }


            list.forEach(booking -> {
                Unit unit = booking.getUnit();
                booking.setCustomerName(booking.getCustomer().getName());
                booking.setCustomerId(booking.getCustomer().getCustomerId());
                booking.setUnitSerial(unit.getSerialNo());


                Optional<Floor> optionalFloor = floorRepo.findById(unit.getFloorId());
                if (optionalFloor.isPresent()) {
                    String projectName = projectRepo.findProjectNameById(optionalFloor.get().getProjectId());
                    booking.setProject(projectName);
                    booking.setFloorNo(String.valueOf(optionalFloor.get().getFloor()));
                }

              Optional<CustomerPayable> customerPayableOptional =
                      customerPayableRepository.findByBooking_IdAndUnit_Id(booking.getId() , unit.getId());

                if (customerPayableOptional.isPresent()){
                    CustomerPayable customerPayable = customerPayableOptional.get();
                    booking.setCustomerPayableId(customerPayable.getId());
                    booking.setTotalCancelPaid(customerPayable.getTotalPaid());
                    booking.setTotalCancelDeductions(customerPayable.getTotalDeductions());
                    booking.setTotalCancelRefund(customerPayable.getTotalRefund());
                    booking.setTotalCancelPayable(customerPayable.getTotalPayable());
                    booking.setTotalCancelBalanceAmount(customerPayable.getBalanceAmount());
                    booking.setCancelledStatus(customerPayable.getStatus());
                }


            });

            return ResponseMapper.buildResponse(Responses.SUCCESS, list);
        }  catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, "No Bookings Found - System Exception");
        }
    }


    @Transactional
    public Map<String, Object> cancelBooking(long bookingId, BookingCancellationRequest request , String loggedInUser) {

        double totalFees = 0;
        double deposited = 0;

        try {

            Optional<Booking> bookingOptional = bookingRepository.findById(bookingId);

            if (bookingOptional.isEmpty() || !bookingOptional.get().isActive()) {
                throw new IllegalArgumentException("Booking doesnt exists or is already cancelled");
            }

            Booking booking = bookingOptional.get();


            Optional<CustomerAccount> customerAccountOptional = customerAccountRepo
                    .findByCustomer_CustomerIdAndUnit_IdAndIsActiveTrue(booking.getCustomerId(), booking.getUnitId());

            if (customerAccountOptional.isEmpty() || !customerAccountOptional.get().isActive()) {
                throw new IllegalArgumentException("Customer Account doesn't exists or is already cancelled");
            }



            CustomerAccount customerAccount = customerAccountOptional.get();

            PaymentSchedule paymentSchedule = paymentScheduleRepository.
                    findByCustomerAccountIdAndPaymentScheduleType(customerAccount.getId() ,
                            PaymentScheduleType.CUSTOMER);

            paymentSchedule.setActive(false);
            paymentSchedule.setUpdatedBy(loggedInUser);
            paymentScheduleRepository.save(paymentSchedule);



            deposited = customerAccount.getTotalPaidAmount();


            for (BookingCancellationRequest.CustomerPayableFeesDto fee : request.getFees()) {
                totalFees += Utility.calculateFee(deposited, fee);
            }
            CustomerPayableDto customerPayableDto = buildCustomerPayableDto(booking, deposited, totalFees, request);

            CustomerPayable customerPayable = CustomerPayable.map(customerPayableDto, booking);

            customerPayable = customerPayableRepository.save(customerPayable);

            booking.setActive(false);
            booking.getUnit().setBooked(false);
            bookingRepository.save(booking);

            customerAccount.setActive(false);
            customerAccountRepo.save(customerAccount);

            customerPayableDto.setId(customerPayable.getId());

            return ResponseMapper.buildResponse(Responses.SUCCESS, customerPayableDto);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
        catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    private CustomerPayableDto buildCustomerPayableDto(Booking booking, double deposited, double totalFees,
                                                       BookingCancellationRequest request) {

        double balance = deposited - totalFees;

        return CustomerPayableDto.builder()
                .bookingId(booking.getId())
                .customerId(booking.getCustomerId())
                .unitId(booking.getUnitId())
                .totalPayable(deposited)
                .totalDeductions(totalFees)
                .totalRefund(balance > 0 ? balance : 0)
                .totalPaid(0)
                .balanceAmount(balance)
                .reason(request.getReason())
                .status(String.valueOf(PENDING))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .details(null)
                .build();
    }

}
