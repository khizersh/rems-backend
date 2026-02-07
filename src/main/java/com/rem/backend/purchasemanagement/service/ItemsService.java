package com.rem.backend.purchasemanagement.service;

import com.rem.backend.purchasemanagement.entity.items.Items;
import com.rem.backend.purchasemanagement.entity.items.ItemsUnit;
import com.rem.backend.purchasemanagement.repository.ItemsRepo;
import com.rem.backend.purchasemanagement.repository.ItemsUnitRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ItemsService {

    private final ItemsRepo itemsRepo;
    private final ItemsUnitRepo itemsUnitRepository;

//    ITEMS

    public Map<String, Object> getAllItems(Long orgId, Pageable pageable) {
        try {

            Page<Items> items = itemsRepo.findByOrganizationId(orgId, pageable);

            if (items != null && !items.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, items);
            }

            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAllItems(long orgId) {
        try {

            List<Items> items = itemsRepo.findAll();

            if (items != null && !items.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, items);
            }

            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    public Map<String, Object> getItemById(long id) {
        try {

            ValidationService.validate(id, "id");

            Optional<Items> itemOptional = itemsRepo.findById(id);

            if (itemOptional.isPresent()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, itemOptional.get());
            }

            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }


    }


    public Map<String, Object> addItem(Items input, Long unitId, String loggedInUser) {
        try {

            ValidationService.validate(input.getName(), "item name");
            ValidationService.validate(input.getCode(), "item code");
            ValidationService.validate(unitId, "unit id");

            Optional<ItemsUnit> unitOptional = itemsUnitRepository.findById(unitId);
            if (unitOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Items Unit");

            ItemsUnit unit = unitOptional.get();

            // ✅ Check duplicate item code
            Optional<Items> existingItem = itemsRepo.findByOrganizationIdAndCode(input.getOrganizationId(), input.getCode());
            if (existingItem.isPresent())
                throw new IllegalArgumentException("Item code already exists");

            LocalDateTime now = LocalDateTime.now();

            Items item = new Items();
            item.setName(input.getName());
            item.setCode(input.getCode());
            item.setDescription(input.getDescription());
            item.setItemsUnit(unit);
            item.setOrganizationId(input.getOrganizationId());
            item.setCreatedBy(loggedInUser);
            item.setUpdatedBy(loggedInUser);
            item.setCreatedDate(now);
            item.setUpdatedDate(now);

            Items saved = itemsRepo.save(item);

            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> updateItem(Items input, Long unitId, String loggedInUser) {
        try {

            ValidationService.validate(input.getId(), "item id");
            ValidationService.validate(input.getName(), "item name");
            ValidationService.validate(input.getCode(), "item code");
            ValidationService.validate(unitId, "unit id");

            Optional<Items> itemOptional = itemsRepo.findById(input.getId());
            if (itemOptional.isEmpty())
                throw new IllegalArgumentException("Item not found");

            Optional<ItemsUnit> unitOptional = itemsUnitRepository.findById(unitId);
            if (unitOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Items Unit");

            // ✅ Check duplicate code (excluding current item)
            Optional<Items> existingItem = itemsRepo.findByCodeAndIdNot(input.getCode(), input.getId());
            if (existingItem.isPresent())
                throw new IllegalArgumentException("Item code already exists");

            Items item = itemOptional.get();
            ItemsUnit unit = unitOptional.get();

            item.setName(input.getName());
            item.setCode(input.getCode());
            item.setDescription(input.getDescription());
            item.setItemsUnit(unit);
            item.setUpdatedBy(loggedInUser);
            item.setUpdatedDate(LocalDateTime.now());

            Items updated = itemsRepo.save(item);

            return ResponseMapper.buildResponse(Responses.SUCCESS, updated);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }





//    ITEMS UNIT

    public Map<String, Object> getAllUnits(long orgId) {
        try {

            List<ItemsUnit> units = itemsUnitRepository.findAllByOrganizationId(orgId);

            if (units != null && !units.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, units);
            }

            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAllUnits(long orgId, Pageable pageable) {
        try {

            Page<ItemsUnit> units = itemsUnitRepository.findAllByOrganizationId(orgId, pageable);

            if (units != null && !units.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, units);
            }

            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> createOrUpdateUnit(ItemsUnit input, String loggedInUser) {
        try {

            ValidationService.validate(input.getName(), "unit name");
            ValidationService.validate(input.getSymbol(), "unit symbol");
            ValidationService.validate(input.getOrganizationId(), "organization id");

            LocalDateTime now = LocalDateTime.now();

            ItemsUnit unit;

            if (input.getId() != null) {
                Optional<ItemsUnit> optionalUnit = itemsUnitRepository.findById(input.getId());
                if (optionalUnit.isEmpty())
                    throw new IllegalArgumentException("Items Unit not found");

                unit = optionalUnit.get();
                unit.setName(input.getName());
                unit.setSymbol(input.getSymbol());
                unit.setOrganizationId(input.getOrganizationId());
                unit.setUpdatedBy(loggedInUser);
                unit.setUpdatedDate(now);

            } else {
                unit = new ItemsUnit();
                unit.setName(input.getName());
                unit.setSymbol(input.getSymbol());
                unit.setOrganizationId(input.getOrganizationId());
                unit.setCreatedBy(loggedInUser);
                unit.setUpdatedBy(loggedInUser);
                unit.setCreatedDate(now);
                unit.setUpdatedDate(now);
            }

            ItemsUnit saved = itemsUnitRepository.save(unit);

            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }









    }
