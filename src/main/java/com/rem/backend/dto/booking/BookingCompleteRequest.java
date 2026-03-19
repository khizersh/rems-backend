package com.rem.backend.dto.booking;

import lombok.Data;

@Data
public class BookingCompleteRequest {
    private long bookingId;
    private boolean bookingComplete;
}
