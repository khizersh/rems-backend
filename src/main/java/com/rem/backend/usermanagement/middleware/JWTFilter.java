package com.rem.backend.usermanagement.middleware;

import com.rem.backend.usermanagement.entity.DefaultRolePermissionMapping;
import com.rem.backend.usermanagement.service.DefaultPermissionMappingService;
import com.rem.backend.usermanagement.service.UserService;
import com.rem.backend.usermanagement.utillity.JWTUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Optional;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@Component
@AllArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtils jwtUtil;
    private final UserService userDetailsService;
    private final DefaultPermissionMappingService defaultPermissionMappingService;


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Base endpoints to skip JWT check
        String[] excludedPaths = {
                "/api/user/login",
                "/api/user/send-reset-link",
                "/api/user/verify-reset-link",
                "/api/user/change-password",
        };

        // Allow endpoints that either match exactly OR start with any excluded path (for path variables)
        return java.util.Arrays.stream(excludedPaths)
                .anyMatch(path::startsWith);
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (shouldNotFilter(request)) {
            chain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");
        final String requestUri = request.getRequestURI().toLowerCase();

        String token = null;
        String username = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
            username = jwtUtil.extractUsername(token);
        }

        if (username != null && jwtUtil.validateToken(token, username)) {
//            var userDetails = userDetailsService.loadUserByUsername(username);
//            var authorities = userDetails.getAuthorities();
//
//            Optional<DefaultRolePermissionMapping> role = authorities.stream()
//                    .flatMap(authority -> defaultPermissionMappingService
//                            .getDefaultPermissionsByUsername(authority.getAuthority()).stream())
//                    .filter(defaultRolePermissionMapping -> {
//                        String endpoint = defaultRolePermissionMapping.getEndPoint().toLowerCase();
//                        return endpoint.equals("*") || requestUri.startsWith(endpoint);
//                    })
//                    .findFirst();

           boolean isValid = userDetailsService.isValidCall(username , requestUri);

            if (isValid) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        null, null, null);

                request.setAttribute(LOGGED_IN_USER, username);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("You are not authorized to access this endpoint.");
                return;
            }
        }

//        username = "admin";
//        var userDetails = userDetailsService.loadUserByUsername(username);
//        var authorities = userDetails.getAuthorities();
//
//        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                userDetails, null, authorities);
//
//        request.setAttribute(LOGGED_IN_USER, username);
//        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }
}
