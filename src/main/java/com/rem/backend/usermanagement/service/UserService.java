package com.rem.backend.usermanagement.service;


import com.rem.backend.dto.commonRequest.PasswordResetRequest;
import com.rem.backend.entity.organization.Organization;
import com.rem.backend.entity.sidebar.Sidebar;
import com.rem.backend.repository.OrganizationRepo;
import com.rem.backend.service.EmailService;
import com.rem.backend.service.SidebarService;
import com.rem.backend.usermanagement.dto.AuthRequest;
import com.rem.backend.usermanagement.entity.*;
import com.rem.backend.usermanagement.repository.PasswordResetCodeRepo;
import com.rem.backend.usermanagement.repository.UserRepo;
import com.rem.backend.usermanagement.utillity.JWTUtils;
import com.rem.backend.utility.CodeGenerator;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.rem.backend.utility.Utility.ADMIN_ROLE_ID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {


    private final UserRepo userRepo;
    private final OrganizationRepo organizationRepo;
    private final RoleService roleService;
    private final JWTUtils jwtUtils;
    private final SidebarService sidebarService;
    private final DefaultPermissionMappingService defaultPermissionMappingService;
    private final CustomPermissionMappingService customPermissionMappingService;
    private final EmailService emailService;
    private final PasswordResetCodeRepo passwordResetCodeRepo;

    @Value("${app.reset.password.url}")
    private String resetLinkUrl;

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


    public Map<String, Object> sendResetLink(String email) {

        try {
            ValidationService.validate(email, "email");
            Optional<User> userOptional = userRepo.findByEmailAndIsActiveTrue(email);

            if (userOptional.isPresent()) {

                User user = userOptional.get();

                Optional<Organization>  organizationOptional = organizationRepo.findByOrganizationIdAndIsActiveTrue(user.getOrganizationId());

                if (organizationOptional.isEmpty()) {
                    throw new IllegalArgumentException("Invalid Organization!");
                }

                if (user.getEmail() == null || StringUtils.isEmpty(user.getEmail())){
                    throw new IllegalArgumentException("Invalid email!");
                }

                Optional<PasswordResetCode>  passwordResetCodeOptional = passwordResetCodeRepo.findByEmailAndIsExpiredFalse(email);

                if (passwordResetCodeOptional.isPresent()){
                    PasswordResetCode  passwordResetCode = passwordResetCodeOptional.get();
                    passwordResetCode.setExpired(true);
                    passwordResetCodeRepo.save(passwordResetCode);
                }

                String resetCode = CodeGenerator.generateResetCode();

                String resetLink = resetLinkUrl + "?code=" + resetCode;

                emailService.sendResetPasswordEmail(email , user.getUsername() , resetLink , organizationOptional.get());

                PasswordResetCode passwordResetCode = new PasswordResetCode();
                passwordResetCode.setEmail(email);
                passwordResetCode.setCode(resetCode);
                passwordResetCodeRepo.save(passwordResetCode);

                return ResponseMapper.buildResponse(Responses.SUCCESS, "Check your email!");
            }

            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Invalid Email!");

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }



    public Map<String, Object> verifyResetCode(String code) {

        try {
            ValidationService.validate(code, "code");

            LocalDateTime validFrom = LocalDateTime.now().minusHours(24);
            Optional<PasswordResetCode> codeOptional = passwordResetCodeRepo.findValidCode(code , validFrom);

            if (codeOptional.isPresent()) {

                PasswordResetCode passwordResetCode = codeOptional.get();
                passwordResetCode.setExpired(true);
                passwordResetCodeRepo.save(passwordResetCode);

                return ResponseMapper.buildResponse(Responses.SUCCESS, passwordResetCode);

            }

            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Invalid Code!");

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }



    public Map<String, Object> changePassword(PasswordResetRequest request) {

        try {
            ValidationService.validate(request.getNewPassword(), "new password");
            ValidationService.validate(request.getCode(), "code");
            ValidationService.validate(request.getEmail(), "email");




            Optional<User> userOptional = userRepo.findByEmailAndIsActiveTrue(request.getEmail());

            if (userOptional.isEmpty())
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Invalid Email!");

            User user = userOptional.get();

            user.setPassword(request.getNewPassword());
            user.setUpdatedBy(user.getUsername());
            userRepo.save(user);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "password changed successfully!");


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

            List<String> roleCodeList = new ArrayList<>();
            for (UserRoles role : roles) {
                Optional<Role>  roleOptional = roleService.getRoleById(role.getRoleId());

                if (roleOptional.isPresent()){
                    roleCodeList.add(roleOptional.get().getName());
                }

            }

            if (userOptional.isPresent() && organizationOptional.isPresent()) {
                String token = jwtUtils.generateToken(userOptional.get().getUsername());
                List<Sidebar> sidebarList = sidebarService.getSidebarByRole(request.getUsername());
                response.put("token", token);
                response.put("organization", organizationOptional.get());
                response.put("sidebar", sidebarList);
                response.put("role", roleCodeList);
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
