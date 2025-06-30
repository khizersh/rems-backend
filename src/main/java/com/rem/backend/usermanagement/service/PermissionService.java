package com.rem.backend.usermanagement.service;

import com.rem.backend.usermanagement.entity.Permissions;
import com.rem.backend.usermanagement.repository.PermissionRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class PermissionService {

    private final PermissionRepo permissionRepo;

    public Map<String, Object> addPermission(Permissions permissions , String loggedInUser) {

        try {
            ValidationService.validate(permissions.getCode(), "code");
            ValidationService.validate(permissions.getEndPoint(), "end point");
            if (permissionRepo.existsByCode(permissions.getCode()))
                throw new IllegalArgumentException("code already exist!");
            if (permissionRepo.existsByEndPoint(permissions.getEndPoint()))
                throw new IllegalArgumentException("end point already exist!");

            permissions.setCreatedBy(loggedInUser);
            permissions.setUpdatedBy(loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, permissionRepo.save(permissions));

        } catch (IllegalArgumentException ia) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, ia.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    public Map<String, Object> getPermissions() {

        try {
            return ResponseMapper.buildResponse(Responses.SUCCESS, permissionRepo.findAll());

        }  catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

}
