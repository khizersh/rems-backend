package com.rem.backend.controller;


import com.rem.backend.dto.AuthRequest;
import com.rem.backend.service.RoleService;
import com.rem.backend.usermanagement.entity.Role;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/role/")
@AllArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping("create")
    public Map addRole(@RequestBody Role request) {
        return roleService.createRole(request);
    }



}
