package com.rem.backend.controller;

import com.rem.backend.dto.analytic.CountStateByTenureRequest;
import com.rem.backend.dto.analytic.DateRangeRequest;
import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.service.ProjectAnalysisService;
import com.rem.backend.service.RevenueAnalysisService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/revenue-analytics/")
@AllArgsConstructor
public class RevenueAnalyticController {


    private final RevenueAnalysisService revenueAnalysisService;


    @PostMapping("/getOrgAccount")
    public Map getOrgAccount(@RequestBody FilterPaginationRequest request) {
        return revenueAnalysisService.getOrgAccountRevenue(request);
    }


    @GetMapping("/getAnalyticsByORgId/{orgId}")
    public Map getOrgAccount(@PathVariable long orgId) {
        return revenueAnalysisService.getRevenueDetails(orgId);
    }


    @PostMapping("/getExpenseDetailByTenure")
    public Map getOrgAccountExpenseByTenure(@RequestBody CountStateByTenureRequest request) {
        return revenueAnalysisService.getExpenseDetailByTenure(request);
    }



    @PostMapping("/getOrgAcctDetailByDateRange")
    public Map getOrgAcctDetailByDateRange(@RequestBody DateRangeRequest request) {

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending()
        );

        return revenueAnalysisService.getRevenueDetailByDateRangeAndTransactionType(request , pageable);
    }


    @PostMapping("/getOrgSalesByDate")
    public Map getOrgSalesByDate(@RequestBody DateRangeRequest request) {
        return revenueAnalysisService.getOrganizationSalesByDateRange(request);
    }


}
