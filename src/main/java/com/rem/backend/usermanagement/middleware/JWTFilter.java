package com.rem.backend.usermanagement.middleware;

import com.rem.backend.usermanagement.entity.UserRoleMapper;
import com.rem.backend.usermanagement.service.UserRoleMappingService;
import com.rem.backend.usermanagement.service.UserService;
import com.rem.backend.usermanagement.utillity.JWTUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
@AllArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtils jwtUtil;
    private final UserService userDetailsService;
    private final UserRoleMappingService userRoleMappingService;


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

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());


            for (GrantedAuthority authority : userDetails.getAuthorities()) {
                Set<UserRoleMapper> roleSet = userRoleMappingService.getUserRolesMappers(authority.getAuthority());

                Optional<UserRoleMapper> role = roleSet.stream()
                        .filter(userRoleMapper -> {
                            String endpoint = userRoleMapper.getEndPoint().toLowerCase();
                            String requestUri = request.getRequestURI().toLowerCase();
                            return endpoint.equalsIgnoreCase("*") || requestUri.startsWith(endpoint);
                        })
                        .findFirst();

                if (role.isPresent()) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            }
        }

        chain.doFilter(request, response);
    }
}
