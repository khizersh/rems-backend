package com.rem.backend.service;

import com.rem.backend.entity.organization.Organization;
import com.rem.backend.repository.OrganizationRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class OrganizationService {

    private final OrganizationRepo organizationRepo;

    private Optional<Organization> getOrganizationById(long id) {
        try {
            return organizationRepo.findById(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }


    private Map<String, Object> createOrganization(Organization organization, String loggedInUser) {
        try {
            ValidationService.validate(organization.getName(), "name");
            ValidationService.validate(organization.getAddress(), "address");
            organization.setCreatedBy(loggedInUser);
            organization.setUpdatedBy(loggedInUser);
            Organization organizationSaved = organizationRepo.save(organization);
            return ResponseMapper.buildResponse(Responses.SUCCESS, organizationSaved);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

}
