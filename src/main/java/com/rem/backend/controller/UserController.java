package com.rem.backend.controller;

import com.rem.backend.dto.AuthRequest;
import com.rem.backend.service.UserService;
import com.rem.backend.usermanagement.utillity.JWTUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/")
@AllArgsConstructor
public class UserController {

    private final JWTUtils jwtUtil;

    private final UserService userService;

    @PostMapping("login")
    public String login(@RequestBody AuthRequest request) {

        return jwtUtil.generateToken(request.getUsername());
    }



    @GetMapping("/{username}")
    public Map getUser(@PathVariable String username) {
        return userService.findUserByUsername(username);
    }


    @PostMapping("verify")
    public String verify(@RequestBody AuthRequest request) {

        return jwtUtil.extractUsername(request.getUsername());
    }

}
