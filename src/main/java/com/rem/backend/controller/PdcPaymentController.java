package com.rem.backend.controller;

import com.rem.backend.dto.expense.PdcListRequestDTO;
import com.rem.backend.dto.pdc.PdcPartialPaymentRequest;
import com.rem.backend.entity.pdc.PdcRecord;
import com.rem.backend.service.PdcPaymentService;
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
@AllArgsConstructor
@RequestMapping("/api/payments/pdc/")
public class PdcPaymentController {

    private final PdcPaymentService pdcPaymentService;


    /**
     * Create a new PDC record.
     * Does NOT create expense - only creates PDC record and increases vendor payable.
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPdc(
            @RequestBody PdcRecord pdcRecord, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = pdcPaymentService.createPdcRecord(pdcRecord, loggedInUser);
        return ResponseEntity.ok(response);
    }


    /**
     * Process (clear) a PDC payment.
     * Creates expense → deducts balance → decreases vendor payable → updates status to CLEARED.
     */
    @PostMapping("/process/{id}")
    public ResponseEntity<Map<String, Object>> processPdc(
            @PathVariable long id, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = pdcPaymentService.processPdc(id, loggedInUser);
        return ResponseEntity.ok(response);
    }


    /**
     * Mark a PDC payment as failed.
     * No balance impact — simply updates status to FAILED.
     * Vendor payable remains unchanged.
     */
    @PostMapping("/mark-failed/{id}")
    public ResponseEntity<Map<String, Object>> markFailed(
            @PathVariable long id, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = pdcPaymentService.markFailed(id, loggedInUser);
        return ResponseEntity.ok(response);
    }


    /**
     * List PDC payments with filters: dueToday, upcoming, overdue, all.
     * Supports vendor/project filtering and pagination.
     */
    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> listPdcPayments(
            @RequestBody PdcListRequestDTO requestDTO) {
        Pageable pageable = PageRequest.of(
                requestDTO.getPage(),
                requestDTO.getSize(),
                requestDTO.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(requestDTO.getSortBy()).ascending()
                        : Sort.by(requestDTO.getSortBy()).descending());

        Map<String, Object> response = pdcPaymentService.listPdcPayments(requestDTO, pageable);
        return ResponseEntity.ok(response);
    }


    /**
     * Get a specific PDC payment by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPdcById(@PathVariable long id) {
        Map<String, Object> response = pdcPaymentService.getPdcById(id);
        return ResponseEntity.ok(response);
    }


    /**
     * Make partial payment for a PDC.
     * Allows paying PDC amount partially using different payment methods (Cash, Bank Transfer, etc.)
     * Use case: When cheque date arrives but full amount not available, pay partially via other methods.
     */
    @PostMapping("/partial-payment")
    public ResponseEntity<Map<String, Object>> makePartialPayment(
            @RequestBody PdcPartialPaymentRequest request, HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);
        Map<String, Object> response = pdcPaymentService.makePartialPayment(
                request.getPdcId(),
                request.getAmount(),
                request.getOrganizationAccountId(),
                request.getPaymentType(),
                request.getPaymentDocNo(),
                request.getComments(),
                loggedInUser
        );
        return ResponseEntity.ok(response);
    }
}
