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
    private final UserRoleMappingService userRoleMappingService;


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/user/login"); // Skip JWT check for login
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
            var userDetails = userDetailsService.loadUserByUsername(username);
            var authorities = userDetails.getAuthorities();

            Optional<UserRoleMapper> role = authorities.stream()
                    .flatMap(authority -> userRoleMappingService
                            .getUserRolesMappers(authority.getAuthority()).stream())
                    .filter(userRoleMapper -> {
                        String endpoint = userRoleMapper.getEndPoint().toLowerCase();
                        return endpoint.equals("*") || requestUri.startsWith(endpoint);
                    })
                    .findFirst();

            if (role.isPresent()) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities);

                request.setAttribute(LOGGED_IN_USER, username);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("You are not authorized to access this endpoint.");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
