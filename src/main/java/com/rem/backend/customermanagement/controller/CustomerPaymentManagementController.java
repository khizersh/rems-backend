package com.rem.backend.customermanagement.controller;

import com.rem.backend.customermanagement.service.CustomerPaymentManagementService;
import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.enums.PaymentStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

/**
 * Customer Payment Management Controller
 * Provides payment management APIs for logged-in customer users
 * All endpoints derive customer identity from JWT token
 */
@RestController
@RequestMapping("/api/customer/payments")
@RequiredArgsConstructor
public class CustomerPaymentManagementController {

    private final CustomerPaymentManagementService customerPaymentManagementService;

    /**
     * Get all payments for logged-in customer across all accounts
     */
    @PostMapping("/getAll")
    public ResponseEntity<?> getAllCustomerPayments(@RequestBody FilterPaginationRequest request, HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> response = customerPaymentManagementService.getAllCustomerPayments(loggedInUser, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payments by account ID with pagination
     */
    @PostMapping("/getByAccount/{accountId}")
    public ResponseEntity<?> getPaymentsByAccount(
            @PathVariable Long accountId,
            @RequestBody FilterPaginationRequest request,
            HttpServletRequest httpRequest) {

        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> response = customerPaymentManagementService.getPaymentsByAccount(loggedInUser, accountId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment details by payment ID
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPaymentDetails(@PathVariable Long paymentId, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = customerPaymentManagementService.getPaymentDetails(loggedInUser, paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payments by status with pagination
     */
    @PostMapping("/getByStatus/{status}")
    public ResponseEntity<?> getPaymentsByStatus(
            @PathVariable PaymentStatus status,
            @RequestBody FilterPaginationRequest request,
            HttpServletRequest httpRequest) {

        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> response = customerPaymentManagementService.getPaymentsByStatus(loggedInUser, status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payments by date range with pagination
     */
    @PostMapping("/getByDateRange")
    public ResponseEntity<?> getPaymentsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestBody FilterPaginationRequest request,
            HttpServletRequest httpRequest) {

        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> response = customerPaymentManagementService.getPaymentsByDateRange(loggedInUser, startDate, endDate, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment summary for customer
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getPaymentSummary(HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = customerPaymentManagementService.getCustomerPaymentSummary(loggedInUser);
        return ResponseEntity.ok(response);
    }
}
