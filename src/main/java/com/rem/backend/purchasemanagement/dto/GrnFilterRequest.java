package com.rem.backend.purchasemanagement.dto;

import com.rem.backend.purchasemanagement.enums.GrnStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GrnFilterRequest {
    private Long orgId;             // REQUIRED - Organization ID filter (mandatory)
    private Long poId;              // Optional - Purchase Order ID filter
    private Long vendorId;          // Optional - Vendor ID filter
    private GrnStatus status;       // Optional - GRN Status filter
    private LocalDate startDate;    // Optional - Start date filter
    private LocalDate endDate;      // Optional - End date filter
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdDate";
    private String sortDir = "desc";
}
