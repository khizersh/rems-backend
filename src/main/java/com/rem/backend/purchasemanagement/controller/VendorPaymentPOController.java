package com.rem.backend.purchasemanagement.controller;

import com.rem.backend.dto.commonRequest.CommonPaginationRequest;
import com.rem.backend.purchasemanagement.entity.vendorpayment.VendorPaymentPO;
import com.rem.backend.purchasemanagement.service.VendorPaymentPOService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/vendorPaymentPO/")
@RequiredArgsConstructor
public class VendorPaymentPOController {

    private final VendorPaymentPOService vendorPaymentPOService;

    // Create Vendor Payment
    @PostMapping("/create")
    public Map createPayment(@RequestBody VendorPaymentPO paymentRequest, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return vendorPaymentPOService.createPayment(paymentRequest, loggedInUser);
    }

    // Get Payment by ID
    @GetMapping("/getById/{paymentId}")
    public Map getPaymentById(@PathVariable long paymentId) {
        return vendorPaymentPOService.getById(paymentId);
    }

    // Get Payments by Invoice ID
    @GetMapping("/getByInvoice/{invoiceId}")
    public Map getPaymentsByInvoice(@PathVariable long invoiceId) {
        return vendorPaymentPOService.getByInvoiceId(invoiceId);
    }

    // Get Payments by Vendor
    @PostMapping("/getByVendor/{vendorId}")
    public Map getPaymentsByVendor(@PathVariable long vendorId, @RequestBody CommonPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        return vendorPaymentPOService.getByVendorId(vendorId, pageable);
    }

    // Get all Payments by Organization
    @PostMapping("/{organizationId}/getAll")
    public Map getAllPaymentsByOrg(@PathVariable long organizationId, @RequestBody CommonPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        return vendorPaymentPOService.getAllByOrgId(organizationId, pageable);
    }

    // Get Total Paid Amount for Invoice
    @GetMapping("/getTotalPaid/{invoiceId}")
    public Map getTotalPaidForInvoice(@PathVariable long invoiceId) {
        return vendorPaymentPOService.getTotalPaidForInvoice(invoiceId);
    }

    // Update an existing payment
    @PutMapping("/update/{paymentId}")
    public Map updatePayment(@PathVariable long paymentId, @RequestBody VendorPaymentPO paymentRequest, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return vendorPaymentPOService.updatePayment(paymentId, paymentRequest, loggedInUser);
    }

}
