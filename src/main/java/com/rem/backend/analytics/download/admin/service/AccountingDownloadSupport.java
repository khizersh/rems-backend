package com.rem.backend.analytics.download.admin.service;

import com.rem.backend.analytics.download.shared.dto.ReportLine;
import com.rem.backend.analytics.reporting.admin.query.projection.AccountSummaryProjection;
import com.rem.backend.analytics.reporting.admin.service.AccountingReportSupport;
import com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil.nz;

/**
 * Shared helpers for building downloadable accounting statement rows from ledger aggregates.
 */
public final class AccountingDownloadSupport {

    private AccountingDownloadSupport() {
    }

    public static boolean isIncomeType(String accountType) {
        String t = normalize(accountType);
        return t.contains("INCOME") || t.contains("REVENUE");
    }

    public static boolean isExpenseType(String accountType) {
        String t = normalize(accountType);
        return t.contains("EXPENSE") || t.contains("COST");
    }

    public static boolean isAssetType(String accountType) {
        return normalize(accountType).contains("ASSET");
    }

    public static boolean isLiabilityType(String accountType) {
        return normalize(accountType).contains("LIAB");
    }

    public static boolean isEquityType(String accountType) {
        String t = normalize(accountType);
        return t.contains("EQUIT") || t.contains("CAPITAL");
    }

    public static double naturalBalance(String accountType, double debit, double credit) {
        return AccountingReportSupport.naturalBalance(accountType, debit, credit);
    }

    /**
     * Merges opening, period and closing aggregates into trial-balance rows.
     * Accounts with all-zero balances are omitted.
     */
    public static List<ReportLine> buildTrialBalanceLines(
            List<AccountSummaryProjection> opening,
            List<AccountSummaryProjection> period,
            List<AccountSummaryProjection> closing) {

        Map<String, AccountSummaryProjection> openingMap = index(opening);
        Map<String, AccountSummaryProjection> periodMap = index(period);
        Map<String, AccountSummaryProjection> closingMap = index(closing);

        LinkedHashMap<String, String> keys = new LinkedHashMap<>();
        openingMap.keySet().forEach(k -> keys.put(k, k));
        periodMap.keySet().forEach(k -> keys.put(k, k));
        closingMap.keySet().forEach(k -> keys.put(k, k));

        List<ReportLine> lines = new ArrayList<>();
        for (String key : keys.keySet()) {
            AccountSummaryProjection o = openingMap.get(key);
            AccountSummaryProjection p = periodMap.get(key);
            AccountSummaryProjection c = closingMap.get(key);

            String accountType = firstNonNull(
                    o != null ? o.getAccountType() : null,
                    p != null ? p.getAccountType() : null,
                    c != null ? c.getAccountType() : null);

            double openingBal = o == null ? 0d
                    : naturalBalance(accountType, nz(o.getTotalDebit()), nz(o.getTotalCredit()));
            double periodDebit = p == null ? 0d : nz(p.getTotalDebit());
            double periodCredit = p == null ? 0d : nz(p.getTotalCredit());
            double closingBal = c == null ? 0d
                    : naturalBalance(accountType, nz(c.getTotalDebit()), nz(c.getTotalCredit()));

            if (Math.abs(openingBal) < 0.005 && Math.abs(periodDebit) < 0.005
                    && Math.abs(periodCredit) < 0.005 && Math.abs(closingBal) < 0.005) {
                continue;
            }

            AccountSummaryProjection ref = c != null ? c : (p != null ? p : o);
            lines.add(ReportLine.builder()
                    .accountCode(ref.getAccountCode())
                    .accountName(ref.getAccountName())
                    .accountType(ref.getAccountType())
                    .accountCategory(ref.getAccountCategory())
                    .openingBalance(ReportingFormatUtil.round2(openingBal))
                    .debit(ReportingFormatUtil.round2(periodDebit))
                    .credit(ReportingFormatUtil.round2(periodCredit))
                    .closingBalance(ReportingFormatUtil.round2(closingBal))
                    .build());
        }

        lines.sort(Comparator
                .comparing((ReportLine l) -> l.getAccountType() != null ? l.getAccountType() : "")
                .thenComparing(l -> l.getAccountCategory() != null ? l.getAccountCategory() : "")
                .thenComparing(l -> l.getAccountCode() != null ? l.getAccountCode() : ""));
        return lines;
    }

    public static List<ReportLine> buildStatementLines(
            List<AccountSummaryProjection> accounts,
            java.util.function.Predicate<String> typeFilter) {

        List<ReportLine> lines = new ArrayList<>();
        for (AccountSummaryProjection a : accounts) {
            if (!typeFilter.test(a.getAccountType())) {
                continue;
            }
            double amount = naturalBalance(a.getAccountType(), nz(a.getTotalDebit()), nz(a.getTotalCredit()));
            if (Math.abs(amount) < 0.005) {
                continue;
            }
            lines.add(ReportLine.builder()
                    .accountCode(a.getAccountCode())
                    .accountName(a.getAccountName())
                    .accountType(a.getAccountType())
                    .accountCategory(a.getAccountCategory())
                    .amount(ReportingFormatUtil.round2(amount))
                    .build());
        }
        lines.sort(Comparator
                .comparing((ReportLine l) -> l.getAccountCategory() != null ? l.getAccountCategory() : "")
                .thenComparing(l -> l.getAccountCode() != null ? l.getAccountCode() : ""));
        return lines;
    }

    public static double sumAmounts(List<ReportLine> lines) {
        return ReportingFormatUtil.round2(
                lines.stream().mapToDouble(l -> nz(l.getAmount())).sum());
    }

    private static Map<String, AccountSummaryProjection> index(List<AccountSummaryProjection> list) {
        Map<String, AccountSummaryProjection> map = new LinkedHashMap<>();
        if (list == null) {
            return map;
        }
        for (AccountSummaryProjection a : list) {
            map.put(accountKey(a), a);
        }
        return map;
    }

    private static String accountKey(AccountSummaryProjection a) {
        return a.getAccountCode() != null ? a.getAccountCode() : "";
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toUpperCase();
    }

    @SafeVarargs
    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
