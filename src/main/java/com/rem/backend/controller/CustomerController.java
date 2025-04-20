package com.rem.backend.controller;

import com.rem.backend.dto.customer.CustomerPaginationRequest;
import com.rem.backend.dto.floor.FloorPaginationRequest;
import com.rem.backend.entity.customer.Customer;
import com.rem.backend.repository.CustomerRepo;
import com.rem.backend.service.CustomerService;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


    @PostMapping("/addCustomer")
    public Map addCustomer(@RequestBody Customer customer){
        return customerService.createCustomer(customer);
    }


    @PostMapping("/getByIds")
    public ResponseEntity<?> getProjectsByIds(@RequestBody CustomerPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> projectPage = customerService.getCustomerByIds(request.getId(), request.getFilteredBy(), pageable);
        return ResponseEntity.ok(projectPage);
    }

}
