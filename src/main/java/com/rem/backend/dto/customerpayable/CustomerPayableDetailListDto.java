package com.rem.backend.dto.customerpayable;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerPayableDetailListDto {

    private List<Detail> details;

    @Data
    public static class Detail {
        private long customerPayableId;
        private String paymentType;
        private BigDecimal amount;
        private String chequeNo;
        private LocalDateTime chequeDate;
        private String createdBy;
        private String updatedBy;
    }
}
