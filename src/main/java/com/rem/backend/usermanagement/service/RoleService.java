package com.rem.backend.usermanagement.service;

import com.rem.backend.usermanagement.repository.RoleRepository;
import com.rem.backend.usermanagement.repository.UserRoleRepository;
import com.rem.backend.usermanagement.entity.Role;
import com.rem.backend.usermanagement.entity.UserRoles;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;


    public Map createRole(Role role) {
        try {
            ValidationService.validate(role.getName(), "role name");
            role.setName(role.getName());
            Role roleSaved = roleRepository.save(role);
            return ResponseMapper.buildResponse(Responses.SUCCESS, roleSaved);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Role getRoleByName(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }


    public Set<UserRoles> getUserRoles(long userId) {
        try {
            return userRoleRepository.findByUserId(userId);
        } catch (Exception e) {
            return null;
        }
    }

    public Optional<Role> getRoleById(long roleId) {
        try {
            return roleRepository.findById(roleId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Role> getAll() {
        try {
            return roleRepository.findAll();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }


}
