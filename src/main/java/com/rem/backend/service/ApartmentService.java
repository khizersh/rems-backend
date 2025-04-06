package com.rem.backend.service;

import com.rem.backend.entity.project.Apartment;
import com.rem.backend.entity.project.Floor;
import com.rem.backend.repository.ApartmentRepo;
import com.rem.backend.repository.FloorRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ApartmentService {


    private final ApartmentRepo apartmentRepo;
    private final FloorRepo floorRepo;

    public Map<String, Object> addOrUpdateApartment(Apartment dto) {
        try {
            Optional<Floor>  floorOptional = floorRepo.findById(dto.getFloorId());


            if(floorOptional.isEmpty())  return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);;

            Floor floor = floorOptional.get();
            Apartment apartment;
            if (apartmentRepo.existsById(dto.getId())) {
                apartment = apartmentRepo.findById(dto.getId()).get();

            } else {
                apartment = new Apartment();
                apartment.setFloor(floor);
            }

            apartment.setSerialNo(dto.getSerialNo());
            apartment.setSquareYards(dto.getSquareYards());
            apartment.setRoomCount(dto.getRoomCount());
            apartment.setBathroomCount(dto.getBathroomCount());
            apartment.setAmount(dto.getAmount());
            apartment.setAdditionalAmount(dto.getAdditionalAmount());
            apartment.setApartmentType(dto.getApartmentType());


            Apartment saved = apartmentRepo.save(apartment);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
