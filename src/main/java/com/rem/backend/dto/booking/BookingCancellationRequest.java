package com.rem.backend.dto.booking;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class BookingCancellationRequest {
    private String reason;
    private List<CustomerPayableFeesDto> fees;


    @Getter
    @Setter
    public static class CustomerPayableFeesDto{
        String type;
        String title;
        String value;
    }
}
