package com.rem.backend.analytics.reporting.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * A self-describing table block: {@code columns} carry rendering metadata and each row
 * is a key&rarr;value map keyed by {@link TableColumn#getKey()}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableData {
    private String key;
    private String title;
    private List<TableColumn> columns;
    private List<Map<String, Object>> rows;
}
