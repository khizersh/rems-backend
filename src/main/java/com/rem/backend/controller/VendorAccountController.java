package com.rem.backend.controller;

import com.rem.backend.dto.commonRequest.CommonPaginationRequest;
import com.rem.backend.entity.vendor.VendorAccount;
import com.rem.backend.service.VendorAccountService;
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
@RequestMapping("/api/vendorAccount/")
@AllArgsConstructor
public class VendorAccountController {


    private final VendorAccountService vendorAccountService;

    @PostMapping("/createAccount")
    public Map addVendorAccount(@RequestBody VendorAccount vendorAccount , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return vendorAccountService.createVendorAccount(vendorAccount, loggedInUser);
    }


    @PostMapping("/getVendorAccountsByOrgId")
    public ResponseEntity<?> getAccountByOrgId(@RequestBody CommonPaginationRequest request){
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> projectPage = vendorAccountService.getAllVendorAccounts(request.getId(), pageable);
        return ResponseEntity.ok(projectPage);
    }


    @GetMapping("/getVendorByOrg/{orgId}")
    public ResponseEntity<?> getVendorsByOrgId(@PathVariable long orgId){
        Map<String , Object> projectPage = vendorAccountService.getAllVendorAccountsByOrg(orgId);
        return ResponseEntity.ok(projectPage);
    }


    @PostMapping("/getHistoryByAccountId")
    public ResponseEntity<?> getAccountDetailsByVendorAcctId(@RequestBody CommonPaginationRequest request){
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> projectPage = vendorAccountService.getVendorDetailsByAccount(request.getId(), pageable);
        return ResponseEntity.ok(projectPage);
    }


}
