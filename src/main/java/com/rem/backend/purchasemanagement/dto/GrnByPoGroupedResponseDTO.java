package com.rem.backend.purchasemanagement.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GrnByPoGroupedResponseDTO {
    private Long poId;
    private String poNumber;
    private Long vendorId;
    private String vendorName;
    private Long projectId;
    private String projectName;
    private LocalDateTime poDate;
    private Long totalGrnCount;
    private LocalDateTime lastGrnDate;
    private LocalDateTime firstGrnDate;
}
