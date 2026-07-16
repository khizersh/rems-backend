package com.rem.backend.analytics.reporting.shared.enums;

/**
 * Rendering hint for the frontend charting library.
 * The reporting backend stays presentation-agnostic; it only suggests a chart type.
 */
public enum ChartType {
    LINE,
    AREA,
    BAR,
    STACKED_BAR,
    PIE,
    DONUT
}
