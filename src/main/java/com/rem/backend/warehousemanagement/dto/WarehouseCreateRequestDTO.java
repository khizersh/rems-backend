package com.rem.backend.warehousemanagement.dto;

import com.rem.backend.enums.WarehouseType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class WarehouseCreateRequestDTO {

    @NotBlank(message = "Warehouse name is required")
    private String name;

    @NotBlank(message = "Warehouse code is required")
    private String code;

    @NotNull(message = "Warehouse type is required")
    private WarehouseType warehouseType;

    private Long projectId;

    @NotNull(message = "Organization ID is required")
    private Long organizationId;

    private Boolean active = true;
}
