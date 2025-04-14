package com.rem.backend.controller;

import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.sidebar.Sidebar;
import com.rem.backend.service.ProjectService;
import com.rem.backend.service.SidebarService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/sidebar/")
@AllArgsConstructor
public class SidebarController {

    private final SidebarService sidebarService;

    @GetMapping("/getSidebar/{id}")
    public Map getSidebarById(@PathVariable long id){
        return sidebarService.getSidebarById(id);
    }

    @PostMapping("/add")
    public Map addSidebar(@RequestBody Sidebar sidebar , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return sidebarService.addSidebar( sidebar , loggedInUser);
    }

    @GetMapping("/update")
    public Map updateSidebar(@RequestBody Sidebar sidebar , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return sidebarService.updateSidebar(sidebar.getId() ,sidebar , loggedInUser);
    }

    @GetMapping("/getSidebarByRole")
    public Map getByOrganizationId(@PathVariable long id ){
        return sidebarService.getSidebarById(id);
    }


    @GetMapping("/getSidebarByUsername")
    public List<Sidebar> getSidebarByUsername(HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return sidebarService.getSidebarByRole(loggedInUser);
    }
}
