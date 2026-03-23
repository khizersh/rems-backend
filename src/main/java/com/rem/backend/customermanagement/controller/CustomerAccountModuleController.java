package com.rem.backend.customermanagement.controller;

import com.rem.backend.customermanagement.service.CustomerAccountModuleService;
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
 * Customer Account Controller
 * Provides account management APIs for logged-in customer users
 * All endpoints derive customer identity from JWT token
 */
@RestController
@RequestMapping("/api/customer/accounts")
@RequiredArgsConstructor
public class CustomerAccountModuleController {

    private final CustomerAccountModuleService customerAccountModuleService;

    /**
     * Get all accounts for logged-in customer with pagination
     */
    @PostMapping("/getAll")
    public ResponseEntity<?> getAllCustomerAccounts(@RequestBody FilterPaginationRequest request, HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> response = customerAccountModuleService.getCustomerAccounts(loggedInUser, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get single account details by account ID
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountById(@PathVariable Long accountId, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = customerAccountModuleService.getCustomerAccountById(loggedInUser, accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get customer account summary
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getAccountSummary(HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = customerAccountModuleService.getCustomerAccountSummary(loggedInUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Get accounts by project ID with pagination
     */
    @PostMapping("/getByProject/{projectId}")
    public ResponseEntity<?> getAccountsByProject(
            @PathVariable Long projectId,
            @RequestBody FilterPaginationRequest request,
            HttpServletRequest httpRequest) {

        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> response = customerAccountModuleService.getCustomerAccountsByProject(loggedInUser, projectId, pageable);
        return ResponseEntity.ok(response);
    }
}
