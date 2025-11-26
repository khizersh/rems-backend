package com.rem.backend.service;

import com.rem.backend.dto.booking.BookingCancellationRequest;
import com.rem.backend.dto.customerpayable.CustomerPayableDto;
import org.springframework.stereotype.Service;

@Service
public class BookingCancellationService {

    public CustomerPayableDto cancelBooking(long bookingId, BookingCancellationRequest request){
        return null;
    }
}
