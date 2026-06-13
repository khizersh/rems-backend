package com.rem.backend.customermanagement.service;

import com.rem.backend.customermanagement.repository.CustomerPaymentDetailRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerPaymentDetailService {

    private final CustomerPaymentDetailRepo customerPaymentDetailRepo;



}
