package com.rem.backend.analytics.reporting.shared.filters;

import lombok.Data;

/**
 * Shared, extensible filter payload for all admin reports. Each report only reads the
 * fields it supports; unsupported fields are ignored (no need to over-specialise per report).
 * <p>
 * Date inputs are strings in {@code d-M-yyyy} format (matching the existing
 * {@code Utility.getStartOfDay/getEndOfDay} convention used elsewhere in the codebase).
 */
@Data
public class ReportingFilter {
    private String fromDate;
    private String toDate;
    private Integer year;
    private Integer month;
    private Long projectId;
    private Long propertyId;
    private Long vendorId;
    private Long customerId;
    private Long warehouseId;
    private String bookingStatus;
    private String paymentStatus;
    private String accountType;
    private String accountCategory;
    private String accountCode;
    private Integer topN;
}
