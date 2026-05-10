package com.rem.backend.propertymanagement.controller;

import com.rem.backend.propertymanagement.entity.PropertyPayment;
import com.rem.backend.propertymanagement.entity.PropertyPurchase;
import com.rem.backend.propertymanagement.entity.PropertySeller;
import com.rem.backend.propertymanagement.service.PropertyAssetService;
import com.rem.backend.propertymanagement.service.PropertyPaymentService;
import com.rem.backend.propertymanagement.service.PropertyPurchaseService;
import com.rem.backend.propertymanagement.service.PropertySellerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/property/")
@AllArgsConstructor
public class PropertyManagementController {

    private final PropertySellerService propertySellerService;
    private final PropertyPurchaseService propertyPurchaseService;
    private final PropertyPaymentService propertyPaymentService;
    private final PropertyAssetService propertyAssetService;

    @PostMapping("/seller/add")
    public ResponseEntity<Map<String, Object>> addSeller(@RequestBody PropertySeller seller, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return ResponseEntity.ok(propertySellerService.addSeller(seller, loggedInUser));
    }

    @PostMapping("/seller/update")
    public ResponseEntity<Map<String, Object>> updateSeller(@RequestBody PropertySeller seller, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return ResponseEntity.ok(propertySellerService.updateSeller(seller, loggedInUser));
    }

    @GetMapping("/seller/{id}")
    public ResponseEntity<Map<String, Object>> getSeller(@PathVariable long id) {
        return ResponseEntity.ok(propertySellerService.getSellerById(id));
    }

    @GetMapping("/seller/by-org/{organizationId}")
    public ResponseEntity<Map<String, Object>> listSellers(@PathVariable long organizationId) {
        return ResponseEntity.ok(propertySellerService.listSellersByOrganization(organizationId));
    }

    @PostMapping("/purchase/add")
    public ResponseEntity<Map<String, Object>> addPurchase(@RequestBody PropertyPurchase purchase, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return ResponseEntity.ok(propertyPurchaseService.createPurchase(purchase, loggedInUser));
    }

    @GetMapping("/purchase/{id}")
    public ResponseEntity<Map<String, Object>> getPurchase(@PathVariable long id) {
        return ResponseEntity.ok(propertyPurchaseService.getPurchaseById(id));
    }

    @GetMapping("/purchase/by-org/{organizationId}")
    public ResponseEntity<Map<String, Object>> listPurchases(@PathVariable long organizationId) {
        return ResponseEntity.ok(propertyPurchaseService.listPurchasesByOrganization(organizationId));
    }

    @PostMapping("/payment/add")
    public ResponseEntity<Map<String, Object>> addPayment(@RequestBody PropertyPayment payment, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return ResponseEntity.ok(propertyPaymentService.addPayment(payment, loggedInUser));
    }

    @GetMapping("/payment/by-purchase/{purchaseId}")
    public ResponseEntity<Map<String, Object>> listPayments(@PathVariable long purchaseId) {
        return ResponseEntity.ok(propertyPaymentService.listPaymentsByPurchase(purchaseId));
    }

    @GetMapping("/asset/{id}")
    public ResponseEntity<Map<String, Object>> getAsset(@PathVariable long id) {
        return ResponseEntity.ok(propertyAssetService.getAssetById(id));
    }
}
