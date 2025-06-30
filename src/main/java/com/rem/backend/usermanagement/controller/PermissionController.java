package com.rem.backend.usermanagement.controller;


import com.rem.backend.usermanagement.entity.Permissions;
import com.rem.backend.usermanagement.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/permission/")
@AllArgsConstructor
public class PermissionController {


    private final PermissionService permissionService;

    @PostMapping("add")
    public Map addRole(@RequestBody Permissions permissions, HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return permissionService.addPermission(permissions, loggedInUser);
    }



}
