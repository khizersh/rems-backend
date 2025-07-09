package com.rem.backend.controller;

import com.rem.backend.dto.analytic.CountStateByTenureRequest;
import com.rem.backend.service.AnalyticsService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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





}
