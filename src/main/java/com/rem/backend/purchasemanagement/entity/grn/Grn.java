package com.rem.backend.purchasemanagement.entity.grn;

import com.rem.backend.purchasemanagement.enums.GrnStatus;
import com.rem.backend.enums.ReceiptType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "grn")
@Data
public class Grn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String grnNumber;

    private Long orgId;

    private Long projectId;

    private Long vendorId;

    private Long poId;

    @Enumerated(EnumType.STRING)
    private GrnStatus status;

    private LocalDateTime receivedDate;

    @Enumerated(EnumType.STRING)
    private ReceiptType receiptType;

    private Long warehouseId;

    private Long directConsumeProjectId;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @Transient
    private List<GrnItems> grnItemsList;
}
