package com.rem.backend.analytics.reporting.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Echoes back the filters that were actually applied to a report, including the
 * effective (resolved) date range. Lets the frontend show "showing data for ..." context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedFilters {
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
