package com.rem.backend.dto.customerpayable;

import com.rem.backend.entity.customerpayable.CustomerPayableFeeDetail;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerPayableFeeDetailListDto {

    private List<FeeDetail> feeDetails;

    @Data
    public static class FeeDetail {
        private long feeId;
        private long customerPayableId;
        private String type;          // FIXED / PERCENTILE
        private String title;
        private double inputValue;    // original input
        private double calculatedAmount;
        private boolean deduction;
        private String createdBy;
        private String updatedBy;
        private LocalDateTime createdDate;
    }

    public static CustomerPayableFeeDetailListDto fromEntityList(
            List<CustomerPayableFeeDetail> details
    ) {

        CustomerPayableFeeDetailListDto dto = new CustomerPayableFeeDetailListDto();

        dto.setFeeDetails(
                details.stream().map(detail -> {
                    FeeDetail d = new FeeDetail();
                    d.setFeeId(detail.getId());
                    d.setCustomerPayableId(detail.getCustomerPayable().getId());
                    d.setType(detail.getType().name());
                    d.setTitle(detail.getTitle());
                    d.setInputValue(detail.getInputValue());
                    d.setCalculatedAmount(detail.getCalculatedAmount());
                    d.setDeduction(detail.isDeduction());
                    d.setCreatedBy(detail.getCreatedBy());
                    d.setUpdatedBy(detail.getUpdatedBy());
                    d.setCreatedDate(detail.getCreatedDate());
                    return d;
                }).toList()
        );

        return dto;
    }
}
