package com.rem.backend.warehousemanagement.service;

import com.rem.backend.warehousemanagement.dto.WarehouseCreateRequestDTO;
import com.rem.backend.warehousemanagement.entity.Warehouse;
import com.rem.backend.enums.WarehouseType;
import com.rem.backend.warehousemanagement.repo.WarehouseRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Transactional
    public Map<String, Object> createWarehouse(WarehouseCreateRequestDTO request, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "Logged in user");
            validateWarehouseRequest(request);

            // Check if code already exists for organization
            if (warehouseRepository.existsByCodeAndOrganizationId(request.getCode(), request.getOrganizationId())) {
                throw new IllegalArgumentException("Warehouse code already exists for this organization");
            }

            // Validate project requirement for PROJECT type
            if (request.getWarehouseType() == WarehouseType.PROJECT && request.getProjectId() == null) {
                throw new IllegalArgumentException("Project ID is required for PROJECT warehouse type");
            }

            Warehouse warehouse = new Warehouse();
            warehouse.setName(request.getName());
            warehouse.setCode(request.getCode().toUpperCase());
            warehouse.setWarehouseType(request.getWarehouseType());
            warehouse.setProjectId(request.getProjectId());
            warehouse.setOrganizationId(request.getOrganizationId());
            warehouse.setActive(request.getActive() != null ? request.getActive() : true);
            warehouse.setCreatedBy(loggedInUser);
            warehouse.setUpdatedBy(loggedInUser);

            warehouse = warehouseRepository.save(warehouse);

            return ResponseMapper.buildResponse(Responses.SUCCESS, warehouse);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> updateWarehouse(Long warehouseId, WarehouseCreateRequestDTO request, String loggedInUser) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");
            ValidationService.validate(loggedInUser, "Logged in user");
            validateWarehouseRequest(request);

            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

            // Check if code conflicts with other warehouses
            Optional<Warehouse> existingWarehouse = warehouseRepository.findByCodeAndOrganizationId(
                request.getCode(), request.getOrganizationId());
            if (existingWarehouse.isPresent() && !existingWarehouse.get().getId().equals(warehouseId)) {
                throw new IllegalArgumentException("Warehouse code already exists for this organization");
            }

            // Validate project requirement for PROJECT type
            if (request.getWarehouseType() == WarehouseType.PROJECT && request.getProjectId() == null) {
                throw new IllegalArgumentException("Project ID is required for PROJECT warehouse type");
            }

            warehouse.setName(request.getName());
            warehouse.setCode(request.getCode().toUpperCase());
            warehouse.setWarehouseType(request.getWarehouseType());
            warehouse.setProjectId(request.getProjectId());
            warehouse.setActive(request.getActive() != null ? request.getActive() : warehouse.getActive());
            warehouse.setUpdatedBy(loggedInUser);

            warehouse = warehouseRepository.save(warehouse);

            return ResponseMapper.buildResponse(Responses.SUCCESS, warehouse);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getWarehouseById(Long warehouseId) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");

            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

            return ResponseMapper.buildResponse(Responses.SUCCESS, warehouse);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getWarehousesByOrganization(Long organizationId, Pageable pageable) {
        try {
            ValidationService.validate(organizationId, "Organization ID");

            Page<Warehouse> warehouses = warehouseRepository.findByOrganizationIdAndActiveTrue(organizationId, pageable);

            return ResponseMapper.buildResponse(Responses.SUCCESS, warehouses);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getWarehousesByProject(Long projectId, Pageable pageable) {
        try {
            ValidationService.validate(projectId, "Project ID");

            Page<Warehouse> warehouses = warehouseRepository.findByProjectIdAndActiveTrue(projectId, pageable);

            return ResponseMapper.buildResponse(Responses.SUCCESS, warehouses);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getWarehousesByType(Long organizationId, WarehouseType warehouseType) {
        try {
            ValidationService.validate(organizationId, "Organization ID");
            ValidationService.validate(warehouseType, "Warehouse Type");

            List<Warehouse> warehouses = warehouseRepository.findByOrganizationAndType(organizationId, warehouseType);

            return ResponseMapper.buildResponse(Responses.SUCCESS, warehouses);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> deactivateWarehouse(Long warehouseId, String loggedInUser) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");
            ValidationService.validate(loggedInUser, "Logged in user");

            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

            warehouse.setActive(false);
            warehouse.setUpdatedBy(loggedInUser);
            warehouseRepository.save(warehouse);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Warehouse deactivated successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> activateWarehouse(Long warehouseId, String loggedInUser) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");
            ValidationService.validate(loggedInUser, "Logged in user");

            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

            warehouse.setActive(true);
            warehouse.setUpdatedBy(loggedInUser);
            warehouseRepository.save(warehouse);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Warehouse activated successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ====================== VALIDATION METHODS ======================

    private void validateWarehouseRequest(WarehouseCreateRequestDTO request) {
        ValidationService.validate(request.getName(), "Warehouse name");
        ValidationService.validate(request.getCode(), "Warehouse code");
        ValidationService.validate(request.getWarehouseType(), "Warehouse type");
        ValidationService.validate(request.getOrganizationId(), "Organization ID");

        if (request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse name cannot be empty");
        }
        if (request.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse code cannot be empty");
        }
        if (request.getCode().length() > 20) {
            throw new IllegalArgumentException("Warehouse code cannot exceed 20 characters");
        }
    }
}
