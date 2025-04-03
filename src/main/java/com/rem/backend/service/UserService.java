package com.rem.backend.service;


import com.rem.backend.repository.UserRepo;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.entity.UserRoles;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {



    private final UserRepo userRepo;
    private final RoleService roleService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserDetails user = findUserByUsernameMiddleware(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        return user;
    }

    private UserDetails findUserByUsernameMiddleware(String username) {
        Optional<User> userOptional = userRepo.findByUsername(username);

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


    public Map<String, Object> findUserByUsername(String username) {

        try {
            ValidationService.validate(username , "username");
            Optional<User> userOptional = userRepo.findByUsername(username);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                Set<UserRoles> roles = roleService.getUserRoles(user.getId());

                user.setRoles(roles);

                return ResponseMapper.buildResponse(Responses.SUCCESS, user);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Invalid username!");

        }catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        }
        catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }
}
