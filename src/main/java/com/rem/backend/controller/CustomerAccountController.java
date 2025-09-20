package com.rem.backend.controller;

import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.service.CustomerAccountService;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/customerAccount/")
@AllArgsConstructor
public class CustomerAccountController {

    private final CustomerAccountService customerAccountService;

    @PostMapping("/getByOrganizationId")
    public ResponseEntity<?> getByOrganizationId(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending()
        );

        Page<CustomerAccount> result = customerAccountService.getByOrganizationId(request.getId(), pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getNameIdByOrganizationId/{id}")
    public Map<String, Object> getNameIdByOrganizationIds(@PathVariable long id) {
        return customerAccountService.getNameIdByOrganizationId(id);
    }


    @PostMapping("/getByProjectId")
    public ResponseEntity<?> getByProjectId(@RequestBody FilterPaginationRequest request) {
        ValidationService.validate(request.getId(), "Project ID");

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Page<CustomerAccount> result = customerAccountService.getByProjectId(request.getId(), pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/getByCustomerId")
    public ResponseEntity<?> getByCustomerId(@RequestBody FilterPaginationRequest request) {

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> result = customerAccountService.getByCustomerId(request.getId(), pageable);
        return ResponseEntity.ok(result);
    }




    @PostMapping("/getByUnitId")
    public ResponseEntity<?> getByUnitId(@RequestBody FilterPaginationRequest request) {

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Page<CustomerAccount> result = customerAccountService.getByUnitId(request.getId(), pageable);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/getAll")
    public ResponseEntity<?> getAll(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Page<CustomerAccount> result = customerAccountService.getAllOrderedByCreatedDateDesc(pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/getByIds")
    public ResponseEntity<?> getCustomerAccountByIds(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> customerAccount = customerAccountService.getCustomerAccountsByIds(request.getId(), request.getFilteredBy(), pageable);
        return ResponseEntity.ok(customerAccount);
    }


    @PostMapping("/getNameIdsByIds")
    public ResponseEntity<?> getCustomerAccountNameIdsByIds(@RequestBody FilterPaginationRequest request) {
        Map<String, Object> customerAccount = customerAccountService.getCustomerAccountsNameIdByIds(request.getId(), request.getFilteredBy());
        return ResponseEntity.ok(customerAccount);
    }

}
