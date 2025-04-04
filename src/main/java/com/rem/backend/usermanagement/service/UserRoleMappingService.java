package com.rem.backend.usermanagement.service;

import com.rem.backend.usermanagement.entity.UserRoleMapper;
import com.rem.backend.usermanagement.repository.UserRoleMappingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
@AllArgsConstructor
public class UserRoleMappingService {

    private final UserRoleMappingRepository userRoleMappingRepository;

    public Set<UserRoleMapper> getUserRolesMappers(String roleCode){
        try{
           return userRoleMappingRepository.findByRoleCode(roleCode);
        }catch (Exception e){
            e.printStackTrace();
           return Collections.emptySet();
        }
    }
}
