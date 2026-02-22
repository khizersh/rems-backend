package com.rem.backend.customermanagement.controller;

import com.rem.backend.customermanagement.service.CustomerLedgerService;
import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
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
 * Customer Ledger Controller
 * Provides ledger management APIs for logged-in customer users
 * All endpoints derive customer identity from JWT token
 */
@RestController
@RequestMapping("/api/customer/ledger")
@RequiredArgsConstructor
public class CustomerLedgerController {

    private final CustomerLedgerService customerLedgerService;

    /**
     * Get complete customer ledger (all transactions across all accounts)
     */
    @PostMapping("/getAll")
    public ResponseEntity<?> getCustomerLedger(@RequestBody FilterPaginationRequest request, HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> response = customerLedgerService.getCustomerLedger(loggedInUser, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get ledger for specific account
     * Returns entries sorted by createdDate ASC (oldest first)
     */
    @PostMapping("/getByAccount/{accountId}")
    public ResponseEntity<?> getAccountLedger(
            @PathVariable Long accountId,
            @RequestBody FilterPaginationRequest request,
            HttpServletRequest httpRequest) {

        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);

        // Force createdDate ASC sorting for chronological order (oldest first)
        String sortBy = "createdDate";
        String sortDir = "asc";

        // Override request parameters to ensure oldest entries appear first
        if (request.getSortBy() == null || request.getSortBy().isEmpty()) {
            request.setSortBy(sortBy);
        }
        if (request.getSortDir() == null || request.getSortDir().isEmpty()) {
            request.setSortDir(sortDir);
        }

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(request.getSortBy()).ascending()); // Always ASC for chronological order

        Map<String, Object> response = customerLedgerService.getAccountLedger(loggedInUser, accountId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get ledger by payment type
     */
    @PostMapping("/getByPaymentType/{paymentType}")
    public ResponseEntity<?> getLedgerByPaymentType(
            @PathVariable String paymentType,
            @RequestBody FilterPaginationRequest request,
            HttpServletRequest httpRequest) {

        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> response = customerLedgerService.getLedgerByPaymentType(loggedInUser, paymentType, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get ledger summary
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getLedgerSummary(HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = customerLedgerService.getLedgerSummary(loggedInUser);
        return ResponseEntity.ok(response);
    }
}
