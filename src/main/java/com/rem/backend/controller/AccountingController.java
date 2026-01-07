package com.rem.backend.controller;


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
    public ResponseEntity<?> getAllChartOfAccounts(@PathVariable long organizationId,
                                                   @RequestParam(required = false) String accountGroup,
                                                   @RequestParam(required = false) String accountType,
                                                   HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);

        Map<String, Object> response =
                accountsService.getAllChartOfAccounts(organizationId, accountGroup, accountGroup);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{organizationId}")
    public ResponseEntity<?> createChartOfAccount(
            @PathVariable long organizationId,
            @RequestBody CreateChartOfAccountRequest request
    ) {
        return ResponseEntity.ok(
                accountsService.createChartOfAccount(
                        organizationId, request
                )
        );
    }

}
