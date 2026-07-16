package com.rem.backend.analytics.reporting.shared.util;

import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resolves reporting filters into concrete date ranges and builds month buckets for trends.
 * <p>
 * Resolution precedence:
 * <ol>
 *   <li>explicit {@code fromDate}/{@code toDate} (format {@code d-M-yyyy})</li>
 *   <li>{@code year} (+ optional {@code month})</li>
 *   <li>fallback: trailing 12 months ending today</li>
 * </ol>
 */
public final class ReportingDateUtil {

    /** Hard cap so a pathological range can never explode the trend buckets. */
    private static final int MAX_MONTH_BUCKETS = 24;

    private ReportingDateUtil() {
    }

    public static DateRange resolveRange(ReportingFilter filter) {
        if (filter == null) {
            return trailingMonths(12);
        }

        boolean hasExplicitDates = isNotBlank(filter.getFromDate()) || isNotBlank(filter.getToDate());
        if (hasExplicitDates) {
            LocalDateTime start = parseStart(filter.getFromDate());
            LocalDateTime end = parseEnd(filter.getToDate());
            return new DateRange(start, end);
        }

        if (filter.getYear() != null) {
            int year = filter.getYear();
            if (filter.getMonth() != null) {
                YearMonth ym = YearMonth.of(year, filter.getMonth());
                return new DateRange(ym.atDay(1).atStartOfDay(), endOfDay(ym.atEndOfMonth()));
            }
            return new DateRange(LocalDate.of(year, 1, 1).atStartOfDay(),
                    endOfDay(LocalDate.of(year, 12, 31)));
        }

        return trailingMonths(12);
    }

    /** Trailing range of {@code months} whole months ending at end-of-today. */
    public static DateRange trailingMonths(int months) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusMonths(Math.max(0, months - 1)).withDayOfMonth(1);
        return new DateRange(start.atStartOfDay(), endOfDay(today));
    }

    /** Ordered list of {@link YearMonth} between range start and end (inclusive, capped). */
    public static List<YearMonth> monthBuckets(DateRange range) {
        List<YearMonth> buckets = new ArrayList<>();
        YearMonth cursor = YearMonth.from(range.getStart().toLocalDate());
        YearMonth last = YearMonth.from(range.getEnd().toLocalDate());
        int guard = 0;
        while (!cursor.isAfter(last) && guard < MAX_MONTH_BUCKETS) {
            buckets.add(cursor);
            cursor = cursor.plusMonths(1);
            guard++;
        }
        return buckets;
    }

    /** Short label, e.g. "Jan 2026". */
    public static String monthLabel(YearMonth ym) {
        String month = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        return month + " " + ym.getYear();
    }

    private static LocalDateTime parseStart(String input) {
        // Reuse the existing codebase convention (d-M-yyyy); blank -> far past.
        return com.rem.backend.utility.Utility.getStartOfDay(input);
    }

    private static LocalDateTime parseEnd(String input) {
        return com.rem.backend.utility.Utility.getEndOfDay(input);
    }

    private static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(23, 59, 59, 999_999_999);
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
