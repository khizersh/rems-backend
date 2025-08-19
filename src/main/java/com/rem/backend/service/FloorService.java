package com.rem.backend.service;

import com.rem.backend.entity.project.Unit;
import com.rem.backend.entity.project.Floor;
import com.rem.backend.entity.project.Project;
import com.rem.backend.repository.FloorRepo;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.repository.UnitRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@Service
public class FloorService {


    private final ProjectRepo projectRepo;
    private final FloorRepo floorRepo;
    private final UnitRepo unitRepo;


    public Map<String, Object> deleteById(long floorId) {
        try {
            ValidationService.validate(floorId, "floor id");
            floorRepo.deleteById(floorId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, "Successfully deleted");
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getFloorsByProject(long projectId, Pageable pageable) {
        try {
            ValidationService.validate(projectId, "project id");
            Page<Floor> floors = floorRepo.findByProjectId(projectId, pageable);
            Optional<Project> projectOptional = projectRepo.findByProjectIdAndIsActiveTrue(projectId);
            if (projectOptional.isPresent()) {
                floors.getContent().forEach(floor -> {
                    int unitCount = unitRepo.countByFloorId(floor.getId());
                    floor.setUnitCount(unitCount);
                    floor.setProjectName(projectOptional.get().getName());
                });
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, floors);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> addOrUpdateFloorInProject( Floor floorInput, String loggedInUser) {
        try {


            ValidationService.validate(floorInput.getProjectId() , "project");
            ValidationService.validate(floorInput.getFloor() , "floor no");

            Optional<Project> projectOptional = projectRepo.findById(floorInput.getProjectId());
            if (projectOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Project!");


            if (floorInput.getUnitList() != null) {
                for (Unit unit : floorInput.getUnitList()) {
                    ValidationService.validate(unit.getSerialNo(), "unit serial no");
                    ValidationService.validate(unit.getSquareFoot(), "square yards");
                    ValidationService.validate(unit.getUnitType(), "unit type");

                    unit.setUpdatedBy(loggedInUser);
                }
            }

            Floor savedFloor;
            if (floorInput.getId() != 0 && floorRepo.existsById(floorInput.getId())) {
                // Update existing floor
                Floor existing = floorRepo.findById(floorInput.getId()).get();
                existing.setFloor(floorInput.getFloor());
                existing.setUnitList(floorInput.getUnitList()); // Will overwrite apartments
                savedFloor = floorRepo.save(existing);
            } else {
                // Add new floor
               Optional<Floor> floorOptional = floorRepo.findByFloorAndProjectId(floorInput.getFloor() , floorInput.getProjectId());
               if (floorOptional.isPresent())
                   throw new IllegalArgumentException("Floor No Already exist!");
                floorInput.setCreatedBy(loggedInUser);
                floorInput.setUpdatedBy(loggedInUser);
                savedFloor = floorRepo.save(floorInput);

                Project project = projectOptional.get();
                project.setFloors(project.getFloors() + 1);
                projectRepo.save(project);

            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, savedFloor);


        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getAllFloorsByProjectId(long projectId ) {
        try {
            ValidationService.validate(projectId, "project id");
            List list = floorRepo.findAllFloorByProjectId(projectId );
            return ResponseMapper.buildResponse(Responses.SUCCESS, list);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
