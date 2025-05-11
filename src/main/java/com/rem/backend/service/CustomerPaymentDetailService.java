package com.rem.backend.service;

import com.rem.backend.repository.CustomerPaymentDetailRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerPaymentDetailService {

    private final CustomerPaymentDetailRepo customerPaymentDetailRepo;



}
