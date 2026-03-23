package com.rem.backend.warehousemanagement.dto;

import com.rem.backend.enums.ReceiptType;
import lombok.Data;

@Data
public class GrnWarehouseRequestDTO {

    private Long grnId;
    private ReceiptType receiptType;
    private Long warehouseId;
    private Long directConsumeProjectId;
}
