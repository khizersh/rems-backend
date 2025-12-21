package com.rem.backend.dto.customerpayable;

import com.rem.backend.entity.customerpayable.CustomerPayable;
import com.rem.backend.entity.customerpayable.CustomerPayableDetail;
import lombok.Data;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CustomerPayableDto {

    private Long id;

    private Long bookingId;
    private Long customerId;
    private Long unitId;

    private double totalPayable;
    private double totalRefund;
    private double totalDeductions;
    private double totalPaid;
    private double balanceAmount;

    private String reason;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private CustomerPayableDetailListDto details;
    private CustomerPayableFeeDetailListDto feeDetails;



    public static CustomerPayableDto map(CustomerPayable customerPayable, CustomerPayableDetailListDto detailListDto,
                                         CustomerPayableFeeDetailListDto feeDetails) {
        return CustomerPayableDto.builder()
                .id(customerPayable.getId())
                .bookingId(customerPayable.getBooking().getId())
                .customerId(customerPayable.getCustomer().getCustomerId())
                .unitId(customerPayable.getUnit().getId())
                .totalPayable(customerPayable.getTotalPayable())
                .totalRefund(customerPayable.getTotalRefund())
                .totalDeductions(customerPayable.getTotalDeductions())
                .totalPaid(customerPayable.getTotalPaid())
                .balanceAmount(customerPayable.getBalanceAmount())
                .reason(customerPayable.getReason())
                .status(customerPayable.getStatus())
                .createdAt(customerPayable.getCreatedAt())
                .updatedAt(customerPayable.getUpdatedAt())
                .details(detailListDto)
                .feeDetails(feeDetails)
                .build();
    }


}

