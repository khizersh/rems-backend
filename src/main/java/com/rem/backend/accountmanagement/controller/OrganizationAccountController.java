package com.rem.backend.accountmanagement.controller;

import com.rem.backend.dto.analytic.DateRangeRequest;
import com.rem.backend.dto.commonRequest.CommonPaginationRequest;
import com.rem.backend.dto.orgAccount.TransferFundRequest;
import com.rem.backend.accountmanagement.entity.OrganizationAccount;
import com.rem.backend.accountmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.accountmanagement.service.OrganizationAccountService;
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
@RequestMapping("/api/organizationAccount/")
@AllArgsConstructor
public class OrganizationAccountController {

    private final OrganizationAccountService organizationAccountService;

    @PostMapping("/createAccount")
    public Map addOrganizationAccount(@RequestBody OrganizationAccount  organizationAccount , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return organizationAccountService.addAccountByOrg(organizationAccount , loggedInUser);
    }


    @GetMapping("/getById/{acctId}")
    public Map getAccountById(@PathVariable long acctId ){
        return organizationAccountService.getOrgAccountsById(acctId);
    }


    @GetMapping("/getAccountByOrgId/{orgId}")
    public Map getAccountByOrgId(@PathVariable long orgId ){
        return organizationAccountService.getOrgAccountsByOrgId(orgId);
    }

    @PostMapping("/addAccountDetail")
    public Map addOrganizationAccountDetail(@RequestBody OrganizationAccountDetail organizationAccountDetail , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return organizationAccountService.addOrgAcctDetail(organizationAccountDetail , loggedInUser);
    }



    @PostMapping("/transferAmount")
    public Map transferAmount(@RequestBody TransferFundRequest transferFundRequest , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return organizationAccountService.transferFund(transferFundRequest , loggedInUser);
    }


    @PostMapping("/{organizationId}/transferAmount")
    public Map addOrDeductBalanceByCategory(@RequestBody OrganizationAccountDetail organizationAccountDetail ,
                                            @PathVariable Long organizationId , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return organizationAccountService.addOrDeductBalance(organizationAccountDetail ,organizationId, loggedInUser);
    }

    @PostMapping("/getAccountDetailByAcctId")
    public ResponseEntity<?> getProjectsByOrganization(@RequestBody CommonPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> projectPage = organizationAccountService.getOrgAccountDetailByOrgAcctId(request.getId(), pageable);
        return ResponseEntity.ok(projectPage);
    }


    @GetMapping("/getAccountDetailByAcctIdPrint/{accountId}")
    public ResponseEntity<?> getProjectsByOrganization(@PathVariable long accountId) {
        Map<String , Object> projectPage = organizationAccountService.getOrgAccountDetailByOrgAcctIdWithoutPagination(accountId);
        return ResponseEntity.ok(projectPage);
    }


    @PostMapping("/getAccountDetailByDateRange")
    public ResponseEntity<?> getAccountDetailByDateRange(@RequestBody DateRangeRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> projectPage = organizationAccountService.getAccountDetailsByDateRangeAndByAccount(request, pageable);
        return ResponseEntity.ok(projectPage);
    }


    @PostMapping("/getAccountDetailByDateRangePrint")
    public ResponseEntity<?> getAccountDetailByDateRangeWithoutPagination(@RequestBody DateRangeRequest request) {
        Map<String , Object> projectPage = organizationAccountService.getAccountDetailsByDateRangeAndByAccountWithoutPagination(request);
        return ResponseEntity.ok(projectPage);
    }
}
