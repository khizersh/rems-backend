package com.rem.backend.analytics.reporting.shared.util;

import com.rem.backend.analytics.reporting.shared.enums.TrendDirection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Small, dependency-free helpers for building dashboard-friendly values:
 * compact number formatting ("1.23M"), null-safe coercion, and trend/percent math.
 */
public final class ReportingFormatUtil {

    /** Below this absolute change %, a trend is considered FLAT. */
    private static final double FLAT_THRESHOLD = 0.5;

    private ReportingFormatUtil() {
    }

    /** Null-safe {@link Double} to primitive (null -&gt; 0.0). */
    public static double nz(Double value) {
        return value == null ? 0.0 : value;
    }

    /** Null-safe {@link Number} to primitive double (null -&gt; 0.0). */
    public static double nz(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    /** Round to 2 decimals (half-up), avoiding floating point noise in payloads. */
    public static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Compact, human-readable number, e.g. 1_234_567.89 -&gt; "1.23M", 12_300 -&gt; "12.3K".
     * Keeps the sign for negative values.
     */
    public static String compact(double value) {
        double abs = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        DecimalFormat df = new DecimalFormat("#.##");
        if (abs >= 1_000_000_000d) {
            return sign + df.format(abs / 1_000_000_000d) + "B";
        }
        if (abs >= 1_000_000d) {
            return sign + df.format(abs / 1_000_000d) + "M";
        }
        if (abs >= 1_000d) {
            return sign + df.format(abs / 1_000d) + "K";
        }
        return sign + df.format(abs);
    }

    /** Plain grouped number, e.g. 1234567 -&gt; "1,234,567". */
    public static String number(double value) {
        return new DecimalFormat("#,##0.##").format(value);
    }

    /**
     * Period-over-period change in percent. Returns {@code null} when there is no
     * meaningful baseline (previous == 0), so the frontend can hide the delta badge.
     */
    public static Double changePercent(double current, double previous) {
        if (previous == 0d) {
            return null;
        }
        return round2(((current - previous) / Math.abs(previous)) * 100d);
    }

    /** Map a change percent to a {@link TrendDirection}. Null change -&gt; FLAT. */
    public static TrendDirection trend(Double changePercent) {
        if (changePercent == null) {
            return TrendDirection.FLAT;
        }
        if (changePercent > FLAT_THRESHOLD) {
            return TrendDirection.UP;
        }
        if (changePercent < -FLAT_THRESHOLD) {
            return TrendDirection.DOWN;
        }
        return TrendDirection.FLAT;
    }
}
