package com.rem.backend.purchasemanagement.entity.vendorinvoice;

import com.rem.backend.purchasemanagement.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vendor_invoice")
@Data
public class VendorInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;

    private Long orgId;
    private Long projectId;
    private Long vendorId;

    private Long poId;
    private Long grnId;

    private Double totalAmount;

    private Double paidAmount;

    private Double pendingAmount;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status; // UNPAID, PARTIAL, PAID

    private LocalDate invoiceDate;

    private LocalDate dueDate;


    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @Transient
    private List<VendorInvoiceItem> invoiceItemList;
}
