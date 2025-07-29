package com.rem.backend.controller;

import com.rem.backend.dto.analytic.CountStateByTenureRequest;
import com.rem.backend.service.AnalyticsService;
import com.rem.backend.service.ProjectAnalysisService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/project-analytics/")
@AllArgsConstructor
public class ProjectAnalyticController {


    private final ProjectAnalysisService projectAnalysisService;


    @GetMapping("/get/{projectId}")
    public Map getAnalytics(@PathVariable long projectId) {
        return projectAnalysisService.getProjectAnalyticsByid(projectId);
    }


    @GetMapping("/getProjectSales/{projectId}")
    public Map getProjectSales(@PathVariable long projectId) {
        return projectAnalysisService.getProjectSalesByid(projectId);
    }


    @GetMapping("/getProjectReceivedAmount/{projectId}")
    public Map getProjectReceivedAmount(@PathVariable long projectId) {
        return projectAnalysisService.getProjectRecievedAmountByid(projectId);
    }


    @GetMapping("/getProjectClientCount/{projectId}")
    public Map getProjectClientCount(@PathVariable long projectId) {
        return projectAnalysisService.getProjectClientCountByid(projectId);
    }



    @GetMapping("/getProjectExpense/{projectId}")
    public Map getProjectExpense(@PathVariable long projectId) {
        return projectAnalysisService.getProjectExpensePurchaseAndPaid(projectId);
    }




}
