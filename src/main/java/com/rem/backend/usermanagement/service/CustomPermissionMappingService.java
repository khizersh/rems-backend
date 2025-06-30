package com.rem.backend.usermanagement.service;

import com.rem.backend.usermanagement.entity.CustomRoleMapping;
import com.rem.backend.usermanagement.entity.DefaultRolePermissionMapping;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.entity.UserRoles;
import com.rem.backend.usermanagement.repository.CustomRoleMappingRepo;
import com.rem.backend.usermanagement.repository.DefaultRolePermissionRepo;
import com.rem.backend.usermanagement.repository.PermissionRepo;
import com.rem.backend.usermanagement.repository.UserRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class CustomPermissionMappingService {

    private final CustomRoleMappingRepo customRoleMappingRepo;
    private final UserRepo userRepo;
    private final RoleService roleService;
    private final PermissionRepo permissionRepo;

    public Set<CustomRoleMapping> getCustomPermissionsByUsername(String username) {
        Set<CustomRoleMapping> list = new HashSet<>();

        try {
            Optional<User> userOptional = userRepo.findByUsernameAndIsActiveTrue(username);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                Set<UserRoles> roles = roleService.getUserRoles(user.getId());
                for (UserRoles role : roles) {
                    list.addAll(customRoleMappingRepo.findByRoleIdAndUserId(role.getRoleId(), role.getUserId()));
                }
            }

            list.forEach(customRole -> {
                permissionRepo.findById(customRole.getPermissionId())
                        .ifPresent(permission -> customRole.setEndPoint(permission.getEndPoint()));
            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return list;
        }
    }
}
