package com.rem.backend.warehousemanagement.controller;

import com.rem.backend.warehousemanagement.dto.WarehouseCreateRequestDTO;
import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.enums.WarehouseType;
import com.rem.backend.warehousemanagement.service.WarehouseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping("/create")
    public Map<String, Object> createWarehouse(@Valid @RequestBody WarehouseCreateRequestDTO request,
                                              HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);
        return warehouseService.createWarehouse(request, loggedInUser);
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateWarehouse(@PathVariable Long id,
                                              @Valid @RequestBody WarehouseCreateRequestDTO request,
                                              HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);
        return warehouseService.updateWarehouse(id, request, loggedInUser);
    }

    @GetMapping("/get/{id}")
    public Map<String, Object> getWarehouseById(@PathVariable Long id) {
        return warehouseService.getWarehouseById(id);
    }

    @PostMapping("/getByOrganization")
    public ResponseEntity<?> getWarehousesByOrganization(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> warehousePage = warehouseService.getWarehousesByOrganization(request.getId(), pageable);
        return ResponseEntity.ok(warehousePage);
    }

    @PostMapping("/getByProject")
    public ResponseEntity<?> getWarehousesByProject(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> warehousePage = warehouseService.getWarehousesByProject(request.getId(), pageable);
        return ResponseEntity.ok(warehousePage);
    }

    @GetMapping("/organization/{organizationId}/type/{warehouseType}")
    public Map<String, Object> getWarehousesByType(@PathVariable Long organizationId,
                                                   @PathVariable WarehouseType warehouseType) {
        return warehouseService.getWarehousesByType(organizationId, warehouseType);
    }

    @PostMapping("/deactivate/{id}")
    public Map<String, Object> deactivateWarehouse(@PathVariable Long id,
                                                   HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);
        return warehouseService.deactivateWarehouse(id, loggedInUser);
    }

    @PostMapping("/activate/{id}")
    public Map<String, Object> activateWarehouse(@PathVariable Long id,
                                                 HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);
        return warehouseService.activateWarehouse(id, loggedInUser);
    }
}
