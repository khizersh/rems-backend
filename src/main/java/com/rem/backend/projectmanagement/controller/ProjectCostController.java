package com.rem.backend.projectmanagement.controller;

import com.rem.backend.dto.projectcost.CancelProjectCostRequest;
import com.rem.backend.dto.projectcost.ClearProjectCostPdcRequest;
import com.rem.backend.dto.projectcost.CreateProjectCostPaymentRequest;
import com.rem.backend.dto.projectcost.CreateProjectCostRequest;
import com.rem.backend.dto.projectcost.ProjectCostFilterRequest;
import com.rem.backend.dto.projectcost.UpdateProjectCostRequest;
import com.rem.backend.projectmanagement.service.ProjectCostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/project-cost")
@AllArgsConstructor
public class ProjectCostController {

    private final ProjectCostService projectCostService;

    @PostMapping("/add")
    public Map<String, Object> createProjectCost(
            @RequestBody CreateProjectCostRequest request, HttpServletRequest servletRequest) {
        return projectCostService.createProjectCost(request, loggedInUser(servletRequest));
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateProjectCost(
            @PathVariable long id,
            @RequestParam long organizationId,
            @RequestBody UpdateProjectCostRequest request,
            HttpServletRequest servletRequest) {
        return projectCostService.updateProjectCost(
                id, organizationId, request, loggedInUser(servletRequest));
    }

    @GetMapping("/{id}")
    public Map<String, Object> getProjectCost(
            @PathVariable long id, @RequestParam long organizationId) {
        return projectCostService.getProjectCostById(id, organizationId);
    }

    @GetMapping("/project/{projectId}")
    public Map<String, Object> getProjectCosts(
            @PathVariable long projectId,
            @RequestParam long organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean payableOnly) {
        return projectCostService.getProjectCostsByProject(projectId, organizationId, page, size, payableOnly);
    }

    @PostMapping("/filter")
    public Map<String, Object> filterProjectCosts(@RequestBody ProjectCostFilterRequest request) {
        return projectCostService.filterProjectCosts(request);
    }

    @PostMapping("/{id}/payment")
    public Map<String, Object> createProjectCostPayment(
            @PathVariable long id,
            @RequestBody CreateProjectCostPaymentRequest request,
            HttpServletRequest servletRequest) {
        return projectCostService.createProjectCostPayment(id, request, loggedInUser(servletRequest));
    }

    @PostMapping("/{id}/clear-pdc")
    public Map<String, Object> clearProjectCostPdc(
            @PathVariable long id,
            @RequestBody ClearProjectCostPdcRequest request,
            HttpServletRequest servletRequest) {
        return projectCostService.clearProjectCostPdc(id, request, loggedInUser(servletRequest));
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancelProjectCost(
            @PathVariable long id,
            @RequestBody CancelProjectCostRequest request,
            HttpServletRequest servletRequest) {
        return projectCostService.cancelProjectCost(id, request, loggedInUser(servletRequest));
    }

    @GetMapping("/project/{projectId}/summary")
    public Map<String, Object> getProjectCostSummary(
            @PathVariable long projectId, @RequestParam long organizationId) {
        return projectCostService.getProjectCostSummary(projectId, organizationId);
    }

    private String loggedInUser(HttpServletRequest request) {
        return (String) request.getAttribute(LOGGED_IN_USER);
    }
}
