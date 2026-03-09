package com.rem.backend.purchasemanagement.dto;

import com.rem.backend.purchasemanagement.enums.GrnStatus;
import com.rem.backend.purchasemanagement.enums.GrnInvoiceStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GrnFilterRequest {
    private Long orgId;                      // REQUIRED - Organization ID filter (mandatory)
    private Long poId;                       // Optional - Purchase Order ID filter
    private Long vendorId;                   // Optional - Vendor ID filter
    private GrnStatus status;                // Optional - GRN Status filter
    private LocalDate startDate;             // Optional - Start date filter
    private LocalDate endDate;               // Optional - End date filter
    private GrnInvoiceStatus invoiceStatus;  // Optional - Invoice status filter (NOT_INVOICED, PARTIALLY_INVOICED, FULLY_INVOICED, null: all)
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdDate";
    private String sortDir = "desc";
}
