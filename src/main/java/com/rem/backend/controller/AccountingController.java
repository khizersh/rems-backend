package com.rem.backend.controller;


import com.rem.backend.dto.accounting.CreateAccountGroupRequest;
import com.rem.backend.dto.accounting.CreateChartOfAccountRequest;
import com.rem.backend.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/accounting/")
@AllArgsConstructor
public class AccountingController {

    private final AccountService accountsService;

    @GetMapping("/{organizationId}/allChartOfAccounts")
    public ResponseEntity<?> getAllChartOfAccounts(@PathVariable Long organizationId,
                                                   @RequestParam(required = false) Long accountGroup,
                                                   @RequestParam(required = false) Long accountType,
                                                   HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);

        Map<String, Object> response =
                accountsService.getAllChartOfAccounts(organizationId, accountType, accountGroup);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{organizationId}/getAccountGroups")
    public ResponseEntity<?> getAccountGroup(@PathVariable long organizationId,
                                             @RequestParam long accountType,
                                                   HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);

        Map<String, Object> response =
                accountsService.getAccountGroups(accountType, organizationId, loggedInUser);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getAllAccountTypes")
    public ResponseEntity<?> getAllAccountTypes(
                                                   HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);

        Map<String, Object> response =
                accountsService.getAllAccountType();

        return ResponseEntity.ok(response);
    }


    @PostMapping("/{organizationId}/expenseChartOfAccount")
    public ResponseEntity<?> createExpenseChartOfAccount(
            @PathVariable long organizationId,
            @RequestBody CreateChartOfAccountRequest createChartOfAccountRequest,
            HttpServletRequest request
    ) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);

        return ResponseEntity.ok(
                accountsService.createExpenseChartOfAccount(
                        organizationId, createChartOfAccountRequest, loggedInUser
                )
        );
    }


    @PostMapping("/{organizationId}/accountGroup")
    public ResponseEntity<?> createAccountGroup(
            @PathVariable long organizationId,
            @RequestBody CreateAccountGroupRequest createAccountGroupRequest,
            HttpServletRequest request
    ) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return ResponseEntity.ok(accountsService.createAccountGroup(organizationId, createAccountGroupRequest, loggedInUser));
    }



}
