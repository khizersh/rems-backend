package com.rem.backend.usermanagement.service;

import com.rem.backend.usermanagement.entity.DefaultRolePermissionMapping;
import com.rem.backend.usermanagement.entity.Permissions;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.entity.UserRoles;
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
public class DefaultPermissionMappingService {

    private final DefaultRolePermissionRepo defaultRolePermissionRepo;
    private final UserRepo userRepo;
    private final RoleService roleService;
    private final PermissionRepo permissionRepo;

    public Set<DefaultRolePermissionMapping> getDefaultPermissionsByUsername(String username){
        Set<DefaultRolePermissionMapping> list = new HashSet<>();

        try{
            Optional<User> userOptional = userRepo.findByUsernameAndIsActiveTrue(username);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                Set<UserRoles> roles = roleService.getUserRoles(user.getId());
                for (UserRoles role : roles){
                    list.addAll(defaultRolePermissionRepo.findByRoleId(role.getRoleId()));
                }
            }

            list.forEach(defaultRole -> {
                permissionRepo.findById(defaultRole.getPermissionId())
                        .ifPresent(permission -> defaultRole.setEndPoint(permission.getEndPoint()));
            });


        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            return list;
        }
    }
}
