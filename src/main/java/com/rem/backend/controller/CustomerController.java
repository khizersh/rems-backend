package com.rem.backend.controller;

import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.entity.customer.Customer;
import com.rem.backend.service.CustomerService;
import com.rem.backend.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;


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


    @GetMapping("/detail/{id}")
    public Map getFullCustomerDetails(@PathVariable long id) {
        return customerService.getFullDetailByCustomer(id);
    }


    @PostMapping("search")
    public Map searchCustomersByName(@RequestBody Map<String, String> request) {
        return customerService.searchCustomersByName(request);
    }


    @PostMapping("/addCustomer")
    public Map addCustomer(@RequestBody Customer customer, HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return customerService.createCustomer(customer , loggedInUser);
    }


    @PostMapping("/updateCustomer")
    public Map updateCustomer(@RequestBody Customer customer, HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return customerService.updateCustomer(customer , loggedInUser);
    }


    @GetMapping("/email")
    public ResponseEntity<?> test() {
        emailService.sendEmailAsync("khizeroff61@gmail.com" , "username" , "password");
        return ResponseEntity.ok("Success");
    }


    @PostMapping("/getByIds")
    public ResponseEntity<?> getProjectsByIds(@RequestBody FilterPaginationRequest request) {
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


    @GetMapping("/getByAccountId/{accountId}")
    public Map getCustomerDetailByAccount(@PathVariable long accountId) {
        return customerService.getFullDetailByCustomerAccountId(accountId);
    }

    @GetMapping("/getUnitListDetailsByCustomerId/{cId}")
    public Map getUnitListDetailsByCustomerId(@PathVariable long cId) {
        return customerService.getUnitListByCustomerId(cId);
    }

}
