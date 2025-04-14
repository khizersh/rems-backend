package com.rem.backend.usermanagement.service;


import com.rem.backend.entity.organization.Organization;
import com.rem.backend.entity.sidebar.Sidebar;
import com.rem.backend.repository.OrganizationRepo;
import com.rem.backend.service.SidebarService;
import com.rem.backend.usermanagement.dto.AuthRequest;
import com.rem.backend.usermanagement.repository.UserRepo;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.entity.UserRoles;
import com.rem.backend.usermanagement.utillity.JWTUtils;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {


    private final UserRepo userRepo;
    private final OrganizationRepo organizationRepo;
    private final RoleService roleService;
    private final JWTUtils jwtUtils;
    private final SidebarService sidebarService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserDetails user = findUserByUsernameMiddleware(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        return user;
    }

    private UserDetails findUserByUsernameMiddleware(String username) {
        Optional<User> userOptional = userRepo.findByUsernameAndIsActiveTrue(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            Set<UserRoles> roles = roleService.getUserRoles(user.getId());

            user.setRoles(roles);

            // Convert user roles to GrantedAuthority
            Set<GrantedAuthority> grantedAuthorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getRoleCode()))
                    .collect(Collectors.toSet());

            // Return a Spring Security UserDetails object
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(), // Ensure password is set
                    grantedAuthorities
            );
        }

        throw new UsernameNotFoundException("User not found with username: " + username);
    }


    public Map<String, Object> findUserByUsername(String username, String loggedInUser) {

        try {
            ValidationService.validate(username, "username");
            Optional<User> userOptional = userRepo.findByUsernameAndIsActiveTrue(username);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                Set<UserRoles> roles = roleService.getUserRoles(user.getId());

                user.setRoles(roles);

                return ResponseMapper.buildResponse(Responses.SUCCESS, user);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Invalid username!");

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    public Map<String, Object> login(AuthRequest request) {
        try {

            Map<String, Object> response = new HashMap<>();
            ValidationService.validate(request.getPassword(), "password");
            ValidationService.validate(request.getUsername(), "username");

            Optional<User> userOptional = userRepo.findByUsernameAndIsActiveTrue(request.getUsername());
            Optional<Organization> organizationOptional = organizationRepo.findByOrganizationIdAndIsActiveTrue(userOptional.get().getOrganizationId());

            if (userOptional.isPresent() && organizationOptional.isPresent()) {
                String token = jwtUtils.generateToken(userOptional.get().getUsername());
                List<Sidebar> sidebarList = sidebarService.getSidebarByRole(request.getUsername());
                response.put("token", token);
                response.put("organization", organizationOptional.get());
                response.put("sidebar", sidebarList);
                return ResponseMapper.buildResponse(Responses.SUCCESS, response);
            }
            return ResponseMapper.buildResponse(Responses.INVALID_USER, null);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
