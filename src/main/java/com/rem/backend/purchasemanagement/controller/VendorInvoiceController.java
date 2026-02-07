package com.rem.backend.purchasemanagement.controller;

import com.rem.backend.dto.commonRequest.CommonPaginationRequest;
import com.rem.backend.purchasemanagement.entity.vendorinvoice.VendorInvoice;
import com.rem.backend.purchasemanagement.enums.InvoiceStatus;
import com.rem.backend.purchasemanagement.service.VendorInvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/vendorInvoice/")
@RequiredArgsConstructor
public class VendorInvoiceController {

    private final VendorInvoiceService vendorInvoiceService;

    // Create Invoice against GRN
    @PostMapping("/create")
    public Map createInvoice(@RequestBody VendorInvoice invoiceRequest, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return vendorInvoiceService.createInvoice(invoiceRequest, loggedInUser);
    }

    // Get Invoice by ID
    @GetMapping("/getById/{invoiceId}")
    public Map getInvoiceById(@PathVariable long invoiceId) {
        return vendorInvoiceService.getById(invoiceId);
    }

    // Get all Invoices by Organization
    @PostMapping("/{organizationId}/getAll")
    public Map getAllInvoicesByOrg(@PathVariable long organizationId, @RequestBody CommonPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        return vendorInvoiceService.getAllByOrgId(organizationId, pageable);
    }

    // Get Invoices by Vendor
    @PostMapping("/getByVendor/{vendorId}")
    public Map getInvoicesByVendor(@PathVariable long vendorId, @RequestBody CommonPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        return vendorInvoiceService.getByVendorId(vendorId, pageable);
    }

    // Get Invoices by Status
    @PostMapping("/{organizationId}/getByStatus/{status}")
    public Map getInvoicesByStatus(
            @PathVariable long organizationId,
            @PathVariable String status,
            @RequestBody CommonPaginationRequest request) {

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
        return vendorInvoiceService.getByStatus(organizationId, invoiceStatus, pageable);
    }

    // Get Pending Amount by Vendor
    @GetMapping("/getPendingAmount/{vendorId}")
    public Map getPendingAmountByVendor(@PathVariable long vendorId) {
        return vendorInvoiceService.getPendingAmountByVendor(vendorId);
    }
}
