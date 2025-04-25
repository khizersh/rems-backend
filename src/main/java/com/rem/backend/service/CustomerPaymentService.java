package com.rem.backend.service;

import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.repository.CustomerPaymentRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class CustomerPaymentService {
    private final CustomerPaymentRepo customerPaymentRepo;


    public Map<String, Object> getPaymentsByCustomerAccountId(long customerAccountId, Pageable pageable) {
        try {
            ValidationService.validate(customerAccountId, "customerAccountId");
            Page<CustomerPayment> payments = customerPaymentRepo.findByCustomerAccountId(customerAccountId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, payments);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
