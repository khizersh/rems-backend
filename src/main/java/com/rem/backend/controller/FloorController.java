package com.rem.backend.controller;

import com.rem.backend.dto.floor.FloorPaginationRequest;
import com.rem.backend.service.FloorService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/floor/")
@AllArgsConstructor
public class FloorController {

    FloorService floorService;

    @PostMapping("/getByProjectId")
    public ResponseEntity<?> getProjectsByOrganization(@RequestBody FloorPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> projectPage = floorService.getFloorsByProject(request.getProjectId(), pageable);
        return ResponseEntity.ok(projectPage);
    }

    @GetMapping("/getAllFloorsByProject/{id}")
    public Map getAllFloorsByProject(@PathVariable long id){
        return floorService.getAllFloorsByProjectId(id);
    }
}
