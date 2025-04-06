package com.rem.backend.service;

import com.rem.backend.entity.project.Apartment;
import com.rem.backend.entity.project.Floor;
import com.rem.backend.entity.project.Project;
import com.rem.backend.repository.FloorRepo;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@Service
public class FloorService {



    private final ProjectRepo projectRepo;
    private final FloorRepo floorRepo;

    public Map<String, Object> addOrUpdateFloorInProject(Long projectId, Floor floorInput, String loggedInUser) {
        try {
            Optional<Project> projectOptional = projectRepo.findById(projectId);


            if (projectOptional.isEmpty()) return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

            Project project = projectOptional.get();
            floorInput.setProject(project);

            if (floorInput.getApartmentList() != null) {
                for (Apartment apartment : floorInput.getApartmentList()) {
                    ValidationService.validate(apartment.getSerialNo(), "apartment serial no");
                    ValidationService.validate(apartment.getSquareYards(), "square yards");
                    ValidationService.validate(apartment.getApartmentType(), "apartment type");

                    apartment.setUpdatedBy(loggedInUser);
                    apartment.setFloor(floorInput); // Link each apartment to the floor
                }
            }

            Floor savedFloor;
            if (floorInput.getId() != 0 && floorRepo.existsById(floorInput.getId())) {
                // Update existing floor
                Floor existing = floorRepo.findById(floorInput.getId()).get();
                existing.setFloor(floorInput.getFloor());
                existing.setApartmentList(floorInput.getApartmentList()); // Will overwrite apartments
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
}
