package com.rem.backend.warehousemanagement.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockSummaryDTO {

    private Long warehouseId;
    private String warehouseName;
    private Long itemId;
    private String itemName;
    private BigDecimal quantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal avgRate;
    private BigDecimal totalValue;
}
