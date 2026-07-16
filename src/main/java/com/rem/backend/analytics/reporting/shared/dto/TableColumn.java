package com.rem.backend.analytics.reporting.shared.dto;

import com.rem.backend.analytics.reporting.shared.enums.ValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Column metadata for dynamically-rendered report/drill-down tables.
 * {@code key} matches the property name inside each row map of a {@link TableData}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableColumn {
    private String key;
    private String label;
    private ValueType type;
}
