package com.rem.backend.dto.booking;

import com.rem.backend.enums.FeeType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class BookingCancellationRequest {
    private long customerPayableId;
    private String reason;
    private List<CustomerPayableFeesDto> fees;


    @Getter
    @Setter
    public static class CustomerPayableFeesDto {
        private Long id;
        FeeType type;
        String title;
        double value;
    }
}
