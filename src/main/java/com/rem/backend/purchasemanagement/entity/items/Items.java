package com.rem.backend.purchasemanagement.entity.items;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Items {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;           // e.g. Cement, Steel Rod
    private String code;           // e.g. ITM-0001
    private String description;

    @ManyToOne
    private ItemsUnit itemsUnit;             // kg, ton, bag, sq.ft


    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;
}