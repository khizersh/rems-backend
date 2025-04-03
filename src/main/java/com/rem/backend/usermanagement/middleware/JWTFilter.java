package com.rem.backend.usermanagement.middleware;

import com.rem.backend.service.UserService;
import com.rem.backend.usermanagement.utillity.JWTUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtils jwtUtil;
    private final UserService userDetailsService;

    public JWTFilter(JWTUtils jwtUtil, UserService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/user/login"); // Skip JWT check for login
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 🔥 Bypass JWT check for public endpoints
        if (shouldNotFilter(request)) {
            chain.doFilter(request, response);
            return;
        }



        final String authorizationHeader = request.getHeader("Authorization");

        String token = null;
        String username = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
            username = jwtUtil.extractUsername(token);
        }

        if (username != null && jwtUtil.validateToken(token, username)) {


            var userDetails = userDetailsService.loadUserByUsername(username);

            // Create an authentication token

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            System.out.println("userDetails.getAuthorities() :: " + userDetails.getAuthorities());
            System.out.println("url  :: " + request.getRequestURI());

            for (GrantedAuthority authority : userDetails.getAuthorities()) {
                if(authority.getAuthority().equalsIgnoreCase("ADMIN_ROLE1")){
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            // Set the authentication in the security context




        }

        chain.doFilter(request, response);
    }
}
