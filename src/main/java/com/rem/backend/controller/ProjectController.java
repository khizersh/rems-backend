package com.rem.backend.controller;

import com.rem.backend.entity.project.Project;
import com.rem.backend.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/add")
    public Map addOrganization(@PathVariable Project project , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return projectService.createProject(project , loggedInUser);
    }

    @GetMapping("/deActivate")
    public Map deActivateOrganization(@PathVariable long id , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return projectService.deActivate(id , loggedInUser);
    }

    @GetMapping("/getByOrganization")
    public Map getByOrganizationId(@PathVariable long id ){
        return projectService.getProjectsByOrganizationId(id);
    }
}
