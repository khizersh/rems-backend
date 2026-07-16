package com.rem.backend.analytics.download.shared.enums;

/**
 * Supported export formats for downloadable accounting reports.
 */
public enum ExportFormat {
    JSON,
    CSV;

    public static ExportFormat fromString(String value) {
        if (value == null || value.isBlank()) {
            return JSON;
        }
        try {
            return ExportFormat.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return JSON;
        }
    }
}
