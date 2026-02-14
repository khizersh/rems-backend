package com.rem.backend.warehousemanagement.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class StockTransferRequestDTO {

    @NotNull(message = "From Warehouse ID is required")
    private Long fromWarehouseId;

    @NotNull(message = "To Warehouse ID is required")
    private Long toWarehouseId;

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    private String remarks;
}
