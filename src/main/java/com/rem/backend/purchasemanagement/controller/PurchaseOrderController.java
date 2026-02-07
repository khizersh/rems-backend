package com.rem.backend.purchasemanagement.controller;


import com.rem.backend.dto.commonRequest.CommonPaginationRequest;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrder;
import com.rem.backend.purchasemanagement.repository.PurchaseOrderRepo;
import com.rem.backend.purchasemanagement.service.PurchaseOrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/purchaseOrder/")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;


    @GetMapping("/getById/{poId}")
    public Map getAccountById(@PathVariable long poId ){
        return purchaseOrderService.getById(poId);
    }


    @PostMapping("/{organizationId}/getAll")
    public Map getAllPoByPagination(@PathVariable long organizationId , @RequestBody CommonPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        return purchaseOrderService.getAll(organizationId, pageable);
    }


    @PostMapping("/createPO")
    public Map createPO(@RequestBody PurchaseOrder poRequest, HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return purchaseOrderService.createOrUpdatePO(poRequest, loggedInUser);
    }

    // Approve Purchase Order
    @PostMapping("/approve/{poId}")
    public Map approvePO(@PathVariable long poId, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return purchaseOrderService.approvePO(poId, loggedInUser);
    }

    // Cancel Purchase Order (Soft Delete)
    @PostMapping("/cancel/{poId}")
    public Map cancelPO(@PathVariable long poId, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return purchaseOrderService.cancelPO(poId, loggedInUser);
    }

}
