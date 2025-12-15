package com.rem.backend.dto.customerpayable;

import com.rem.backend.entity.customerpayable.CustomerPayableDetail;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerPayableDetailListDto {

    private List<Detail> details;

    @Data
    public static class Detail {
        private long customerPayableId;
        private String paymentType;
        private double amount;
        private Long organizationAccountId;
        private String chequeNo;
        private LocalDateTime chequeDate;
        private String comments;
        private String createdBy;
        private String updatedBy;
    }


    public static CustomerPayableDetailListDto fromEntityList(List<CustomerPayableDetail> details) {

        CustomerPayableDetailListDto dto = new CustomerPayableDetailListDto();

        dto.setDetails(
                details.stream().map(detail -> {
                    CustomerPayableDetailListDto.Detail d = new CustomerPayableDetailListDto.Detail();
                    d.setCustomerPayableId(detail.getCustomerPayable().getId());
                    d.setPaymentType(detail.getPaymentType());
                    d.setAmount(detail.getAmount());
                    d.setChequeNo(detail.getChequeNo());
                    d.setChequeDate(detail.getChequeDate());
                    d.setCreatedBy(detail.getCreatedBy());
                    d.setUpdatedBy(detail.getUpdatedBy());
                    return d;
                }).toList()
        );

        return dto;
    }
}
