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

    public Map<String, Object> getOrganizationById(long id) {
        try {
            ValidationService.validate(id, "id");
            Optional<Organization> organizationOptional = organizationRepo.findByOrganizationIdAndIsActiveTrue(id);

            if (organizationOptional.isPresent())
                return ResponseMapper.buildResponse(Responses.SUCCESS, organizationOptional.get());

            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> createOrganization(Organization organization, String loggedInUser) {
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

    public Map<String, Object> deActivate(long id, String loggedInUser) {
        try {
            ValidationService.validate(id, "id");
            Optional<Organization> organizationOptional = organizationRepo.findByOrganizationIdAndIsActiveTrue(id);
            if (organizationOptional.isPresent()) {
                Organization organization = organizationOptional.get();
                organization.setUpdatedBy(loggedInUser);
                organization.setActive(false);
                return ResponseMapper.buildResponse(Responses.SUCCESS, organizationRepo.save(organization));
            }
            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

}
