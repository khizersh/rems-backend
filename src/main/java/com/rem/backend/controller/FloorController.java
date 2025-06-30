package com.rem.backend.controller;

import com.rem.backend.dto.floor.FloorPaginationRequest;
import com.rem.backend.entity.project.Floor;
import com.rem.backend.service.FloorService;
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



    @GetMapping("/deleteById/{id}")
    public Map deleteById(@PathVariable long id){
        return floorService.deleteById(id);
    }

    @PostMapping("/addorUpdateFloor")
    public Map addFloor(@RequestBody Floor floor , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return floorService.addOrUpdateFloorInProject(floor , loggedInUser);
    }
}
