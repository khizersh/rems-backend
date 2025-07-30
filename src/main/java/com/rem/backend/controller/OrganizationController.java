package com.rem.backend.controller;

import com.rem.backend.entity.organization.Organization;
import com.rem.backend.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/organization/")
@AllArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/getOrganization/{id}")
    public Map getOrganizationById(@PathVariable long id){
       return organizationService.getOrganizationById(id);
    }

    @PostMapping("/add")
    public Map addOrganization(@RequestBody Organization organization , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return organizationService.createOrganization(organization , loggedInUser);
    }


    @PostMapping("/update")
    public Map updateOrganization(@RequestBody Organization organization , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return organizationService.updateOrganization(organization , loggedInUser);
    }




    @PostMapping("/deActivate")
    public Map deActivateOrganization(@PathVariable long id , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return organizationService.deActivate(id , loggedInUser);
    }
}
