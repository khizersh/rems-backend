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

    public Map<String, Object> addOrUpdateFloorInProject(Long projectId, Floor floorInput, String loggedInUser) {
        try {
            Optional<Project> projectOptional = projectRepo.findById(projectId);


            if (projectOptional.isEmpty()) return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

            Project project = projectOptional.get();

            if (floorInput.getUnitList() != null) {
                for (Unit unit : floorInput.getUnitList()) {
                    ValidationService.validate(unit.getSerialNo(), "unit serial no");
                    ValidationService.validate(unit.getSquareYards(), "square yards");
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
                floorInput.setCreatedBy(loggedInUser);
                floorInput.setUpdatedBy(loggedInUser);
                savedFloor = floorRepo.save(floorInput);
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
