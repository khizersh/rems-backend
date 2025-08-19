package com.rem.backend.controller;

import com.rem.backend.dto.unit.UnitPaginationRequest;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.service.UnitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

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


    @GetMapping("/getIdSerialByFloorId/{floorId}")
    public ResponseEntity<?> getUnitIdSerialByFloorId(@PathVariable long floorId) {
        Map<String , Object> projectPage = unitService.getUnitIdSerialByFloor(floorId);
        return ResponseEntity.ok(projectPage);
    }

    @GetMapping("/getDetailsById/{unitId}")
    public ResponseEntity<?> getUnitDetailsById(@PathVariable long unitId) {
        Map<String , Object> projectPage = unitService.getUnitByUnitId(unitId);
        return ResponseEntity.ok(projectPage);
    }


    @GetMapping("/getUnitDetailsById/{unitId}")
    public ResponseEntity<?> getDetailsById(@PathVariable long unitId) {
        Map<String , Object> projectPage = unitService.getUnitDetailsByUnit(unitId);
        return ResponseEntity.ok(projectPage);
    }

    @PostMapping("/addOrUpdate")
    public ResponseEntity<?> addOrUpdateUnit(@RequestBody Unit unit, HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map<String , Object> response = unitService.addOrUpdateUnit(unit , loggedInUser);
        return ResponseEntity.ok(response);
    }
}
