package com.rem.backend.service;

import com.rem.backend.dto.booking.BookingCancellationRequest;
import com.rem.backend.dto.customerpayable.CustomerPayableDto;
import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.customerpayable.CustomerPayable;
import com.rem.backend.repository.BookingRepository;
import com.rem.backend.repository.CustomerAccountRepo;
import com.rem.backend.repository.CustomerPayableRepository;
import com.rem.backend.utility.Utility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.rem.backend.enums.CustomerPayableStatus.PENDING;

@Service
@RequiredArgsConstructor
public class BookingCancellationService {

    private final BookingRepository bookingRepository;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerPayableRepository customerPayableRepository;

    public CustomerPayableDto cancelBooking(long bookingId, BookingCancellationRequest request) {

        double totalFees = 0;
        double deposited = 0;

        try {

            Optional<Booking> bookingOptional = bookingRepository.findById(bookingId);

            if (bookingOptional.isEmpty() || !bookingOptional.get().isActive()) {
                throw new Exception("Booking doesnt exists or is already cancelled");
            }

            Booking booking = bookingOptional.get();


            Optional<CustomerAccount> customerAccountOptional = customerAccountRepo
                    .findByCustomer_CustomerIdAndUnit_Id(booking.getCustomerId(), booking.getUnitId());

            if (customerAccountOptional.isEmpty() || !customerAccountOptional.get().isActive()) {
                throw new Exception("Customer Account doesnt exists or is already cancelled");
            }

            CustomerAccount customerAccount = customerAccountOptional.get();


            deposited = customerAccount.getDownPayment() +
                    customerAccount.getTotalPaidAmount();


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

            return customerPayableDto;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private CustomerPayableDto buildCustomerPayableDto(Booking booking, double deposited, double totalFees,
                                                       BookingCancellationRequest request) {

        double balance = deposited - totalFees;

        return CustomerPayableDto.builder()
                .bookingId(booking.getId())
                .customerId(booking.getCustomerId())
                .unitId(booking.getUnitId())
                .totalPayable(BigDecimal.valueOf(deposited))
                .totalDeductions(BigDecimal.valueOf(totalFees))
                .totalRefund(BigDecimal.valueOf(balance > 0 ? balance : 0))
                .totalPaid(BigDecimal.valueOf(0))
                .balanceAmount(BigDecimal.valueOf(balance))
                .reason(request.getReason())
                .status(String.valueOf(PENDING))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .details(null)
                .build();
    }

}
