package com.rem.backend.controller;

import com.rem.backend.dto.customer.CustomerPaginationRequest;
import com.rem.backend.service.CustomerPaymentService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/customerPayment/")
@AllArgsConstructor
public class CustomerPaymentController {

    CustomerPaymentService customerPaymentService;

    @PostMapping("/getByCustomerAccountId")
    public ResponseEntity<?> getProjectsByIds(@RequestBody CustomerPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> projectPage = customerPaymentService.getPaymentsByCustomerAccountId(request.getId(),  pageable);
        return ResponseEntity.ok(projectPage);
    }
}
