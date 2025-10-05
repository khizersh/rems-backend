package com.rem.backend.controller;

import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.entity.customer.CustomerPaymentDetail;
import com.rem.backend.service.CustomerPaymentService;
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
@RequestMapping("/api/customerPayment/")
@AllArgsConstructor
public class CustomerPaymentController {

    CustomerPaymentService customerPaymentService;

    @PostMapping("/getByCustomerAccountId")
    public ResponseEntity<?> getProjectsByIds(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> projectPage = customerPaymentService.getPaymentsByCustomerAccountId(request.getId(), pageable);
        return ResponseEntity.ok(projectPage);
    }


    @PostMapping("/payInstallment")
    public ResponseEntity<?> payInstallment(@RequestBody CustomerPayment customerPaymentRequest, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> projectPage = customerPaymentService.updateCustomerPayment(customerPaymentRequest, loggedInUser);
        return ResponseEntity.ok(projectPage);
    }


    @PostMapping("/addPaymentToOrgAccount")
    public ResponseEntity<?> addPaymentToOrganizationAccount(@RequestBody CustomerPayment customerPaymentRequest, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> projectPage = customerPaymentService.addCustomerPaymentToOrgAccount(customerPaymentRequest, loggedInUser);
        return ResponseEntity.ok(projectPage);
    }


    @PostMapping("/updatePayment")
    public ResponseEntity<?> updatePayment(@RequestBody CustomerPaymentDetail customerPaymentRequest, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> projectPage = customerPaymentService.updateCustomerPaymentDetail(customerPaymentRequest, loggedInUser);
        return ResponseEntity.ok(projectPage);
    }


    @PostMapping("/deleteUnPostedPayment")
    public ResponseEntity<?> deleteUnPostedPayment(@RequestBody CustomerPayment customerPaymentRequest, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> projectPage = customerPaymentService.deleteUnpostedPayment(customerPaymentRequest, loggedInUser);
        return ResponseEntity.ok(projectPage);
    }

    @GetMapping("/paymentDetails/{paymentId}")
    public ResponseEntity<?> getDetailsByPaymentId(@PathVariable long paymentId) {
        Map<String, Object> projectPage = customerPaymentService.getPaymentDetailsByPaymentId(paymentId);
        return ResponseEntity.ok(projectPage);
    }


    @GetMapping("/customerLedger/{customerAccountId}")
    public ResponseEntity<?> getAllDetailsByCustomer(@PathVariable long customerAccountId) {
        Map<String, Object> projectPage = customerPaymentService.getAllPaymentDetailsByAccountId(customerAccountId);
        return ResponseEntity.ok(projectPage);
    }


}
