package com.rem.backend.usermanagement.controller;

import com.rem.backend.dto.commonRequest.PasswordResetRequest;
import com.rem.backend.usermanagement.dto.AuthRequest;
import com.rem.backend.usermanagement.service.UserService;
import com.rem.backend.usermanagement.utillity.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

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
    public Map getUser(@PathVariable String username, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return userService.findUserByUsername(username, loggedInUser);
    }


    @GetMapping("/send-reset-link/{email}")
    public Map sendResetLink(@PathVariable String email) {
        return userService.sendResetLink(email);
    }


    @GetMapping("/verify-reset-link/{code}")
    public Map verifyResetLink(@PathVariable String code) {
        return userService.verifyResetCode(code);
    }



    @PostMapping("/change-password")
    public Map changePassword(@RequestBody PasswordResetRequest request) {
        return userService.changePassword(request);
    }


    @PostMapping("verify")
    public String verify(@RequestBody AuthRequest request) {
        return jwtUtil.extractUsername(request.getUsername());
    }

}
