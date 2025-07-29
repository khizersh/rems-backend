package com.rem.backend.controller;

import com.rem.backend.dto.analytic.CountStateByTenureRequest;
import com.rem.backend.service.AnalyticsService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/analytics/")
@AllArgsConstructor
public class AnalyticController {


    private final AnalyticsService analyticsService;


    @PostMapping("/getCount")
    public Map getCounts(@RequestBody CountStateByTenureRequest countStateByTenureRequest) {
        return analyticsService.getCountByState(countStateByTenureRequest);
    }



    @GetMapping("/getBookingCount/{organizationId}")
    public Map getSalesCountByYear(@PathVariable long organizationId) {
        return analyticsService.getBookingCountByYear(organizationId);
    }



    @GetMapping("/getBookingAmountSum/{organizationId}")
    public Map getSalesAmountSumByYear(@PathVariable long organizationId) {
        return analyticsService.getBookingAmountSumByYear(organizationId);
    }


}
