package com.rem.backend.service;

import com.rem.backend.entity.project.Apartment;
import com.rem.backend.entity.project.Floor;
import com.rem.backend.entity.project.Project;
import com.rem.backend.enums.ProjectType;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ProjectService {

    private final ProjectRepo projectRepo;

    public Map<String, Object> getProjectById(@PathVariable long id) {
        try {

            ValidationService.validate(id, "id");
            Optional<Project> projectOptional = projectRepo.findByProjectIdAndIsActiveTrue(id);
            if (projectOptional.isPresent())
                return ResponseMapper.buildResponse(Responses.SUCCESS, projectOptional.get());

            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> createProject(Project project, String loggedInUser) {
        try {
            project.setCreatedBy(loggedInUser);
            project.setUpdatedBy(loggedInUser);
            ValidationService.validate(project.getName(), "name");
            ValidationService.validate(project.getFloors(), "floors");
            ValidationService.validate(project.getAddress(), "address");
            ValidationService.validate(project.getProjectType(), "project type");

            if (project.getProjectType().equals(ProjectType.APARTMENT)) {
                for (Floor floor : project.getFloorList()) {
                    ValidationService.validate(floor.getFloor(), "floor no");
                    floor.setProject(project);

                    for (Apartment apartment : floor.getApartmentList()) {
                        ValidationService.validate(apartment.getSerialNo(), "apartment serial no");
                        ValidationService.validate(apartment.getAmount(), "amount");
                        ValidationService.validate(apartment.getSquareYards(), "square yards");
                        ValidationService.validate(apartment.getApartmentType(), "apartment type");

                        apartment.setFloor(floor); // Link apartment to floor
                    }

                }
            }

            Project projectSaved = projectRepo.save(project);
            return ResponseMapper.buildResponse(Responses.SUCCESS, projectSaved);
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
            Optional<Project> projectOptional = projectRepo.findByProjectIdAndIsActiveTrue(id);
            if (projectOptional.isPresent()) {
                Project project = projectOptional.get();
                project.setUpdatedBy(loggedInUser);
                project.setActive(false);
                return ResponseMapper.buildResponse(Responses.SUCCESS, projectRepo.save(project));
            }
            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String , Object> getProjectsByOrganizationId(long organizationId){
        try{
            ValidationService.validate(organizationId , "organization id");
            List<Project> projects = projectRepo.findByOrganizationId(organizationId);
            return ResponseMapper.buildResponse(Responses.SUCCESS , projects);

        }catch (IllegalArgumentException e){
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        }catch (Exception e){
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }





}
