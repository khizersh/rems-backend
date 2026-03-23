package com.rem.backend.customermanagement.controller;

import com.rem.backend.customermanagement.service.CustomerDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

/**
 * Customer Dashboard Controller
 * Provides dashboard APIs for logged-in customer users
 * All endpoints derive customer identity from JWT token
 */
@RestController
@RequestMapping("/api/customer/dashboard")
@RequiredArgsConstructor
public class CustomerDashboardController {

    private final CustomerDashboardService dashboardService;

    /**
     * Get customer summary - KPIs and overview
     * Returns: customer name, total bookings, total amount, paid, remaining, overdue
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getCustomerSummary(HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = dashboardService.getCustomerSummary(loggedInUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Get monthly payment chart data
     * Returns: monthly aggregated payment data for visualization
     */
    @GetMapping("/payment-chart")
    public ResponseEntity<?> getPaymentChart(HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = dashboardService.getPaymentChartData(loggedInUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment mode distribution
     * Returns: breakdown of payments by mode (Cash, Bank, Cheque, etc.)
     */
    @GetMapping("/payment-modes")
    public ResponseEntity<?> getPaymentModeDistribution(HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = dashboardService.getPaymentModeDistribution(loggedInUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Get recent payment transactions
     * Returns: last N payments with details
     * @param limit - number of recent payments to fetch (default: 10)
     */
    @GetMapping("/recent-payments")
    public ResponseEntity<?> getRecentPayments(
            HttpServletRequest request,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = dashboardService.getRecentPayments(loggedInUser, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all customer accounts with status
     * Returns: list of all units/accounts with payment status
     */
    @GetMapping("/accounts")
    public ResponseEntity<?> getAccountsStatus(HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = dashboardService.getAccountsStatus(loggedInUser);
        return ResponseEntity.ok(response);
    }
}
