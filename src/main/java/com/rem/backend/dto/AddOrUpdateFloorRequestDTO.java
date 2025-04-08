package com.rem.backend.dto;

import com.rem.backend.entity.project.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddOrUpdateFloorRequestDTO {
    private Long projectId;
    private int floor;
    private List<Unit> unitList;
}