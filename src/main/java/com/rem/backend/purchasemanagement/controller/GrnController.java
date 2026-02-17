package com.rem.backend.purchasemanagement.controller;

import com.rem.backend.dto.commonRequest.CommonPaginationRequest;
import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.purchasemanagement.entity.grn.Grn;
import com.rem.backend.purchasemanagement.service.GrnService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/grn/")
@RequiredArgsConstructor
public class GrnController {

    private final GrnService grnService;

    // Create GRN from Purchase Order
    @PostMapping("/create")
    public Map createGrn(@RequestBody Grn grnRequest, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return grnService.createGrn(grnRequest, loggedInUser);
    }

    // Get GRN by ID
    @GetMapping("/getById/{grnId}")
    public Map getGrnById(@PathVariable long grnId) {
        return grnService.getById(grnId);
    }

    // Get all GRNs by Purchase Order ID
    @PostMapping("/getByPoId/{poId}")
    public Map getGrnsByPoId(@PathVariable long poId, @RequestBody CommonPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        return grnService.getByPoId(poId, pageable);
    }

    // Get GRNs grouped by PO ID with count
    @PostMapping("/getGroupedByPoId")
    public Map getGrnGroupedByPoId(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        return grnService.getGrnGroupedByPoId(request.getId(), pageable);
    }
}
