package com.rem.backend.service;


import com.rem.backend.dto.customerpayable.CustomerPayableDetailListDto;
import com.rem.backend.dto.customerpayable.CustomerPayableDto;
import com.rem.backend.entity.customerpayable.CustomerPayable;
import com.rem.backend.entity.customerpayable.CustomerPayableDetail;
import com.rem.backend.enums.CustomerPayableStatus;
import com.rem.backend.repository.CustomerPayableDetailRepository;
import com.rem.backend.repository.CustomerPayableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CustomerPayableService {

    private final CustomerPayableRepository customerPayableRepository;
    private final CustomerPayableDetailRepository customerPayableDetailRepository;

    @Transactional
    public CustomerPayableDto addPaymentDetail(
            long customerPayableId,
            CustomerPayableDetailListDto dtoList) {

        try {
            CustomerPayable customerPayable = customerPayableRepository.findById(customerPayableId)
                    .orElseThrow(() -> new Exception("Customer Payable doesn't exist or is already cancelled"));

            BigDecimal totalPaid = customerPayable.getTotalPaid() == null
                    ? BigDecimal.ZERO
                    : customerPayable.getTotalPaid();

            BigDecimal balance = customerPayable.getBalanceAmount() == null
                    ? BigDecimal.ZERO
                    : customerPayable.getBalanceAmount();

            for (CustomerPayableDetailListDto.Detail d : dtoList.getDetails()) {

                CustomerPayableDetail detail = new CustomerPayableDetail();
                detail.setCustomerPayable(customerPayable);
                detail.setPaymentType(d.getPaymentType());
                detail.setAmount(d.getAmount());
                detail.setChequeNo(d.getChequeNo());
                detail.setChequeDate(d.getChequeDate());
                detail.setCreatedBy(d.getCreatedBy());
                detail.setUpdatedBy(d.getUpdatedBy());


                customerPayableDetailRepository.save(detail);

                totalPaid = totalPaid.add(d.getAmount());
                balance = balance.subtract(d.getAmount());
            }


            customerPayable.setTotalPaid(totalPaid);
            customerPayable.setBalanceAmount(balance);

            if (BigDecimal.ZERO.equals(customerPayable.getBalanceAmount())) {
                customerPayable.setStatus(String.valueOf(CustomerPayableStatus.PAID));
            } else {
                customerPayable.setStatus(String.valueOf(CustomerPayableStatus.UNPAID));
            }

            customerPayableRepository.save(customerPayable);

            return CustomerPayableDto.map(customerPayable, dtoList);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
