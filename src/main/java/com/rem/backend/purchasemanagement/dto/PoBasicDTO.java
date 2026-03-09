// Explanation: New DTO to return minimal PO info (id and poNumber)
package com.rem.backend.purchasemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PoBasicDTO {
    private Long id;
    private String poNumber;
}
