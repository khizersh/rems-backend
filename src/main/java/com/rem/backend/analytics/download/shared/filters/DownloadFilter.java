package com.rem.backend.analytics.download.shared.filters;

import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extends {@link ReportingFilter} with download-specific options.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DownloadFilter extends ReportingFilter {
    /** Export format: JSON (preview) or CSV (file download). Defaults to JSON. */
    private String format;
    /** As-of date for balance sheet (d-M-yyyy). Falls back to {@code toDate} when blank. */
    private String asOfDate;
}
