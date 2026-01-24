package com.rem.backend.purchasemanagement.entity.grn;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "grn_items")
@Data
public class GrnItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long grnId;
    private Long poItemId;

    private Double quantityReceived;
}
