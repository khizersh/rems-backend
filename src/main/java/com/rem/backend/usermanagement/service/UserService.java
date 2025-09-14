package com.rem.backend.usermanagement.service;


import com.rem.backend.entity.organization.Organization;
import com.rem.backend.entity.sidebar.Sidebar;
import com.rem.backend.repository.OrganizationRepo;
import com.rem.backend.service.SidebarService;
import com.rem.backend.usermanagement.dto.AuthRequest;
import com.rem.backend.usermanagement.dto.UserWithPermissionsDto;
import com.rem.backend.usermanagement.entity.CustomRoleMapping;
import com.rem.backend.usermanagement.entity.DefaultRolePermissionMapping;
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

import static com.rem.backend.utility.Utility.ADMIN_ROLE_ID;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {


    private final UserRepo userRepo;
    private final OrganizationRepo organizationRepo;
    private final RoleService roleService;
    private final JWTUtils jwtUtils;
    private final SidebarService sidebarService;
    private final DefaultPermissionMappingService defaultPermissionMappingService;
    private final CustomPermissionMappingService customPermissionMappingService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

//        UserDetails user = findUserByUsernameMiddleware(username);
        UserDetails user = null;

        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        return user;
    }

    public boolean isValidCall(String username, String requestUri) {
        boolean flag = false;
        try {

            Optional<String> endPointExist = getFinalUserPermissionsByUsername(username).
                    stream().filter(endpoint ->
                            endpoint.equals("/api/*") || requestUri.toLowerCase().startsWith(endpoint.toLowerCase())
                    ).findFirst();

            if (endPointExist.isPresent())
                flag = true;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return flag;
        }
    }

    private List<String> getFinalUserPermissionsByUsername(String username) {
        try {

            Set<DefaultRolePermissionMapping> defaultSet = defaultPermissionMappingService.getDefaultPermissionsByUsername(username);
            Set<CustomRoleMapping> customSet = customPermissionMappingService.getCustomPermissionsByUsername(username);

// Step 1: Remove inactive custom permissions from defaultSet
            Set<DefaultRolePermissionMapping> toRemove = customSet.stream().filter(custom -> !custom.isActive()).map(custom -> {
                DefaultRolePermissionMapping temp = new DefaultRolePermissionMapping();
                temp.setPermissionId(custom.getPermissionId());
                temp.setRoleId(custom.getRoleId());
                return temp;
            }).collect(Collectors.toSet());

            defaultSet.removeAll(toRemove);

// Step 2: Add active custom permissions if not already in defaultSet
            Set<DefaultRolePermissionMapping> toAdd = customSet.stream().filter(CustomRoleMapping::isActive).map(custom -> {
                DefaultRolePermissionMapping temp = new DefaultRolePermissionMapping();
                temp.setPermissionId(custom.getPermissionId());
                temp.setRoleId(custom.getRoleId());
                return temp;
            }).filter(item -> !defaultSet.contains(item)).collect(Collectors.toSet());

            defaultSet.addAll(toAdd);


            List<String> endPointList = defaultSet.stream().map(defaultRolePermissionMapping -> defaultRolePermissionMapping.getEndPoint()).toList();

            return endPointList;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

//    private UserDetails findUserByUsernameMiddleware(String username) {
//        Optional<User> userOptional = userRepo.findByUsernameAndIsActiveTrue(username);
//
//        if (userOptional.isPresent()) {
//            User user = userOptional.get();
//
//            Set<UserRoles> roles = roleService.getUserRoles(user.getId());
//
//            user.setRoles(roles);
//
//            // Convert user roles to GrantedAuthority
//            Set<GrantedAuthority> grantedAuthorities = user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getRoleId().toString())).collect(Collectors.toSet());
//
//            // Return a Spring Security UserDetails object
//            return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), // Ensure password is set
//                    grantedAuthorities);
//        }
//
//        throw new UsernameNotFoundException("User not found with username: " + username);
//    }


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
            if (userOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Credentials!");

            Optional<Organization> organizationOptional = organizationRepo.findByOrganizationIdAndIsActiveTrue(userOptional.get().getOrganizationId());


            String roleShortCode = "ur";
            Set<UserRoles> roles = roleService.getUserRoles(userOptional.get().getId());
            for (UserRoles role : roles){
                if (role.getRoleId() == ADMIN_ROLE_ID){
                    roleShortCode = "ar";
                    break;
                }
            }

            if (userOptional.isPresent() && organizationOptional.isPresent()) {
                String token = jwtUtils.generateToken(userOptional.get().getUsername());
                List<Sidebar> sidebarList = sidebarService.getSidebarByRole(request.getUsername());
                response.put("token", token);
                response.put("organization", organizationOptional.get());
                response.put("sidebar", sidebarList);
                response.put("r", roleShortCode);
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
