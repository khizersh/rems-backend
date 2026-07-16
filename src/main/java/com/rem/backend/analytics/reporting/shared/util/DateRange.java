package com.rem.backend.analytics.reporting.shared.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Resolved, inclusive date range used to scope reporting queries.
 */
@Data
@AllArgsConstructor
public class DateRange {
    private LocalDateTime start;
    private LocalDateTime end;
}
