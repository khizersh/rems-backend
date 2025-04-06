package com.rem.backend.controller;

import com.rem.backend.entity.customer.Customer;
import com.rem.backend.repository.CustomerRepo;
import com.rem.backend.service.CustomerService;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/customer/")
@AllArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/{id}")
    public Map getCustomerById(@PathVariable long id){
        return customerService.getCustomerById(id);
    }

}
