package com.rem.backend.controller;

import com.rem.backend.dto.customer.CustomerPaginationRequest;
import com.rem.backend.dto.floor.FloorPaginationRequest;
import com.rem.backend.entity.customer.Customer;
import com.rem.backend.repository.CustomerRepo;
import com.rem.backend.service.CustomerService;
import com.rem.backend.service.EmailService;
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
    private final EmailService emailService;

    @GetMapping("/{id}")
    public Map getCustomerById(@PathVariable long id) {
        return customerService.getCustomerById(id);
    }


    @PostMapping("search")
    public Map searchCustomersByName(@RequestBody Map<String, String> request) {
        return customerService.searchCustomersByName(request);
    }


    @PostMapping("/addCustomer")
    public Map addCustomer(@RequestBody Customer customer) {
        return customerService.createCustomer(customer);
    }


    @GetMapping("/email")
    public ResponseEntity<?> test() {
        emailService.sendEmailAsync("khizeroff61@gmail.com" , "username" , "password");
        return ResponseEntity.ok("Success");
    }


    @PostMapping("/getByIds")
    public ResponseEntity<?> getProjectsByIds(@RequestBody CustomerPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> projectPage = customerService.getCustomerByIds(request.getId(), request.getFilteredBy(), pageable);
        return ResponseEntity.ok(projectPage);
    }


    @PostMapping("/getFullDetailsByCustomerId")
    public Map getCustomerFullDetailsByCustomerId(@RequestBody Map<String , String> request) {
        return customerService.getFullDetailByCustomerId(request);
    }

}
