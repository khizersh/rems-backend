package com.rem.backend.controller;

import com.rem.backend.dto.floor.FloorPaginationRequest;
import com.rem.backend.dto.unit.UnitPaginationRequest;
import com.rem.backend.service.FloorService;
import com.rem.backend.service.UnitService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/unit/")
@AllArgsConstructor
public class UnitController {

    UnitService unitService;

    @PostMapping("/getByFloorId")
    public ResponseEntity<?> getUnitByFloorId(@RequestBody UnitPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> projectPage = unitService.getUnitByFloor(request.getFloorId(), pageable);
        return ResponseEntity.ok(projectPage);
    }
}
