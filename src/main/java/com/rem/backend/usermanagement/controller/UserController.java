package com.rem.backend.usermanagement.controller;

import com.rem.backend.usermanagement.dto.AuthRequest;
import com.rem.backend.usermanagement.service.UserService;
import com.rem.backend.usermanagement.utillity.JWTUtils;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/")
@AllArgsConstructor
public class UserController {

    private final JWTUtils jwtUtil;

    private final UserService userService;

    @PostMapping("login")
    public Map login(@RequestBody AuthRequest request) {
        return userService.login(request);
    }


    @GetMapping("/getUser/{username}")
    public Map getUser(@PathVariable String username) {
        return userService.findUserByUsername(username);
    }


    @PostMapping("verify")
    public String verify(@RequestBody AuthRequest request) {

        return jwtUtil.extractUsername(request.getUsername());
    }

}
