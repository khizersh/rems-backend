package com.rem.backend.service;

import com.rem.backend.entity.project.Unit;
import com.rem.backend.entity.project.Floor;
import com.rem.backend.entity.project.Project;
import com.rem.backend.enums.ProjectType;
import com.rem.backend.repository.FloorRepo;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.repository.UnitRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.transaction.Transactional;
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
    private final FloorRepo floorRepo;
    private final UnitRepo unitRepo;

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



    @Transactional
    public Map<String, Object> createProject(Project project, String loggedInUser) {
        try {
            // Set user information for project, floors, and units
            project.setCreatedBy(loggedInUser);
            project.setUpdatedBy(loggedInUser);

            // Validate project details
            ValidationService.validate(project.getName(), "name");
            ValidationService.validate(project.getFloors(), "floors");
            ValidationService.validate(project.getAddress(), "address");
            ValidationService.validate(project.getProjectType(), "project type");

            // If project type is APARTMENT, validate floors and units
            if (project.getProjectType().equals(ProjectType.APARTMENT)) {
                for (Floor floor : project.getFloorList()) {
                    ValidationService.validate(floor.getFloor(), "floor no");
                    floor.setCreatedBy(loggedInUser);
                    floor.setUpdatedBy(loggedInUser);

                    for (Unit unit : floor.getUnitList()) {
                        ValidationService.validate(unit.getSerialNo(), "unit serial no");
                        ValidationService.validate(unit.getAmount(), "amount");
                        ValidationService.validate(unit.getSquareYards(), "square yards");
                        ValidationService.validate(unit.getUnitType(), "unit type");
                        unit.setCreatedBy(loggedInUser);
                        unit.setUpdatedBy(loggedInUser);
                        unitRepo.save(unit);
                    }
                    floorRepo.save(floor);
                }

            }

            // Save the project (which will cascade save for floors and units)
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

    public Map<String, Object> getProjectsByOrganizationId(long organizationId , Pageable pageable) {
        try {
            ValidationService.validate(organizationId, "organization id");
            Page<Project> projects = projectRepo.findByOrganizationId(organizationId , pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, projects);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


}
