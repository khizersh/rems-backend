package com.rem.backend.service;

import com.rem.backend.dto.unit.UnitDetails;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.entity.project.Floor;
import com.rem.backend.enums.PaymentScheduleType;
import com.rem.backend.repository.PaymentScheduleRepository;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.repository.UnitRepo;
import com.rem.backend.repository.FloorRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.List;

@Service
@AllArgsConstructor
public class UnitService {


    private final UnitRepo unitRepo;
    private final FloorRepo floorRepo;
    private final ProjectRepo projectRepo;
    private final PaymentScheduleRepository paymentScheduleRepository;


    public Map<String, Object> getUnitByFloor(long floorId, Pageable pageable) {
        try {
            ValidationService.validate(floorId, "floor id");
            Page<Unit> units = unitRepo.findByFloorId(floorId, pageable);

            units.getContent().forEach(unit -> {
                floorRepo.findById(unit.getFloorId()).ifPresent(floor -> {
                    unit.setFloorNo(floor.getFloor());
                    projectRepo.findById(floor.getProjectId()).ifPresent(project -> {
                        unit.setProjectName(project.getName());
                    });
                });
            });


            return ResponseMapper.buildResponse(Responses.SUCCESS, units);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getUnitDetailsByFloor(long unitId) {
        try {
            ValidationService.validate(unitId, "unit id");

            UnitDetails unitDetails = null;

            Optional<Unit> unitOptional = unitRepo.findById(unitId);
            if (unitOptional.isPresent()) {
                Optional<Floor> optionalFloor = floorRepo.findById(unitOptional.get().getFloorId());
                if (optionalFloor.isPresent()) {
                    String projectName = projectRepo.findProjectNameById(optionalFloor.get().getProjectId());

                    Optional<PaymentSchedule>  paymentScheduleOptional = paymentScheduleRepository.findByUnitIdAndPaymentScheduleType(unitId , PaymentScheduleType.BUILDER);

                    if(paymentScheduleOptional.isPresent()){
                        unitDetails = new UnitDetails();
                        unitDetails.setUnitId(unitOptional.get().getId());
                        unitDetails.setUnitSerial(unitOptional.get().getSerialNo());
                        unitDetails.setFloorId(optionalFloor.get().getId());
                        unitDetails.setFloorNo(optionalFloor.get().getFloor());
                        unitDetails.setProjectId(optionalFloor.get().getProjectId());
                        unitDetails.setProjectName(projectName);
                        unitDetails.setTotalAmount(paymentScheduleOptional.get().getTotalAmount());
                    }

                }
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, unitDetails);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getUnitIdSerialByFloor(long floorId) {
        try {
            ValidationService.validate(floorId, "floor id");
            List<Map<String, Object>> units = unitRepo.findAllUnitByFloorIdAndIsBookedFalse(floorId);
            UnitDetails unitDetails = new UnitDetails();
            return ResponseMapper.buildResponse(Responses.SUCCESS, units);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> addOrUpdateUnit(Unit dto) {
        try {
            Optional<Floor> floorOptional = floorRepo.findById(dto.getFloorId());


            if (floorOptional.isEmpty()) return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

            Floor floor = floorOptional.get();
            Unit unit;
            if (unitRepo.existsById(dto.getId())) {
                unit = unitRepo.findById(dto.getId()).get();

            } else {
                unit = new Unit();
            }

            unit.setSerialNo(dto.getSerialNo());
            unit.setSquareYards(dto.getSquareYards());
            unit.setRoomCount(dto.getRoomCount());
            unit.setBathroomCount(dto.getBathroomCount());
            unit.setAmount(dto.getAmount());
            unit.setAdditionalAmount(dto.getAdditionalAmount());
            unit.setUnitType(dto.getUnitType());


            Unit saved = unitRepo.save(unit);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

}
