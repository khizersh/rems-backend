package com.rem.backend.controller;

import com.rem.backend.dto.project.ProjectPaginationRequest;
import com.rem.backend.entity.project.Project;
import com.rem.backend.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/project/")
@AllArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/getProject/{id}")
    public Map getProjectById(@PathVariable long id){
        return projectService.getProjectById(id);
    }

    @GetMapping("/getAllProjectByOrg/{id}")
    public Map getAllProjectsByOrganizationId(@PathVariable long id){
        return projectService.getAllProjectsByOrgId(id);
    }

    @PostMapping("/add")
    public Map addProject(@RequestBody Project project , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return projectService.createProject(project , loggedInUser);
    }


    @PostMapping("/update")
    public Map updateProject(@RequestBody Project project , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return projectService.updateProject(project , loggedInUser);
    }

    @GetMapping("/deActivate")
    public Map deActivateOrganization(@PathVariable long id , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return projectService.deActivate(id , loggedInUser);
    }


    @PostMapping("/getByOrganization")
    public ResponseEntity<?> getProjectsByOrganization(@RequestBody ProjectPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> projectPage = projectService.getProjectsByOrganizationId(request.getOrganizationId(), pageable);
        return ResponseEntity.ok(projectPage);
    }
}
