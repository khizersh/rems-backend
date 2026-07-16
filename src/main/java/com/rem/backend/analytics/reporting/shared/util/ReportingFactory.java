package com.rem.backend.analytics.reporting.shared.util;

import com.rem.backend.analytics.reporting.shared.dto.AppliedFilters;
import com.rem.backend.analytics.reporting.shared.dto.KpiCard;
import com.rem.backend.analytics.reporting.shared.dto.LabeledValue;
import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.enums.TrendDirection;
import com.rem.backend.analytics.reporting.shared.enums.ValueType;
import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;

/**
 * Convenience factory for the most common dashboard building blocks, so report services
 * stay focused on data composition rather than DTO boilerplate.
 */
public final class ReportingFactory {

    private ReportingFactory() {
    }

    public static AppliedFilters appliedFilters(ReportingFilter f) {
        if (f == null) {
            return AppliedFilters.builder().build();
        }
        return AppliedFilters.builder()
                .fromDate(f.getFromDate())
                .toDate(f.getToDate())
                .year(f.getYear())
                .month(f.getMonth())
                .projectId(f.getProjectId())
                .propertyId(f.getPropertyId())
                .vendorId(f.getVendorId())
                .customerId(f.getCustomerId())
                .warehouseId(f.getWarehouseId())
                .bookingStatus(f.getBookingStatus())
                .paymentStatus(f.getPaymentStatus())
                .accountType(f.getAccountType())
                .accountCategory(f.getAccountCategory())
                .accountCode(f.getAccountCode())
                .topN(f.getTopN())
                .build();
    }

    public static KpiCard currencyCard(String key, String label, double value, String icon, ColorHint color) {
        return currencyCard(key, label, value, icon, color, null);
    }

    public static KpiCard currencyCard(String key, String label, double value, String icon,
                                       ColorHint color, Double changePercent) {
        return KpiCard.builder()
                .key(key)
                .label(label)
                .value(ReportingFormatUtil.round2(value))
                .formattedValue(ReportingFormatUtil.compact(value))
                .changePercent(changePercent)
                .trend(ReportingFormatUtil.trend(changePercent))
                .colorHint(color)
                .icon(icon)
                .type(ValueType.CURRENCY)
                .build();
    }

    public static KpiCard countCard(String key, String label, double value, String icon, ColorHint color) {
        return KpiCard.builder()
                .key(key)
                .label(label)
                .value(value)
                .formattedValue(ReportingFormatUtil.number(value))
                .trend(TrendDirection.FLAT)
                .colorHint(color)
                .icon(icon)
                .type(ValueType.NUMBER)
                .build();
    }

    public static LabeledValue currencyValue(String key, String label, double value, ColorHint color) {
        return LabeledValue.builder()
                .key(key)
                .label(label)
                .value(ReportingFormatUtil.round2(value))
                .formattedValue(ReportingFormatUtil.compact(value))
                .type(ValueType.CURRENCY)
                .colorHint(color)
                .build();
    }

    public static LabeledValue countValue(String key, String label, double value, ColorHint color) {
        return LabeledValue.builder()
                .key(key)
                .label(label)
                .value(value)
                .formattedValue(ReportingFormatUtil.number(value))
                .type(ValueType.NUMBER)
                .colorHint(color)
                .build();
    }
}
