package com.rem.backend.service;

import com.rem.backend.entity.organization.Organization;
import com.rem.backend.repository.OrganizationRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OrganizationService {

//    private final OrganizationRepo organizationRepo;

    private Organization getOrganizationById(long id){
        try{

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
