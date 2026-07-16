package com.rem.backend.analytics.reporting.admin.service;

import com.rem.backend.analytics.reporting.shared.enums.ColorHint;

/**
 * Pure helpers for interpreting accounting aggregates: normal-balance side per account type
 * and a semantic colour hint per type. Kept separate so financial services stay readable.
 * <p>
 * Account type names come from the {@code account_type} table. Matching is done on
 * case-insensitive substrings so it tolerates naming variants ("LIABILITY"/"LIABILITIES",
 * "INCOME"/"REVENUE", etc.) rather than hardcoding exact strings.
 */
public final class AccountingReportSupport {

    private AccountingReportSupport() {
    }

    /**
     * Natural (normal-side) balance for an account type.
     * <ul>
     *   <li>ASSET, EXPENSE -&gt; debit - credit</li>
     *   <li>LIABILITY, INCOME/REVENUE, EQUITY -&gt; credit - debit</li>
     * </ul>
     * Unknown types default to debit - credit.
     */
    public static double naturalBalance(String accountType, double debit, double credit) {
        if (isCreditNormal(accountType)) {
            return credit - debit;
        }
        return debit - credit;
    }

    public static boolean isCreditNormal(String accountType) {
        String t = normalize(accountType);
        return t.contains("LIAB") || t.contains("INCOME") || t.contains("REVENUE") || t.contains("EQUIT");
    }

    public static ColorHint colorForType(String accountType) {
        String t = normalize(accountType);
        if (t.contains("ASSET")) {
            return ColorHint.PRIMARY;
        }
        if (t.contains("LIAB")) {
            return ColorHint.WARNING;
        }
        if (t.contains("INCOME") || t.contains("REVENUE")) {
            return ColorHint.SUCCESS;
        }
        if (t.contains("EXPENSE") || t.contains("COST")) {
            return ColorHint.DANGER;
        }
        if (t.contains("EQUIT")) {
            return ColorHint.INFO;
        }
        return ColorHint.NEUTRAL;
    }

    /**
     * Semantic colour per account_category name.
     * <p>
     * Actual DB category names (from account_category table):
     * CURRENT ASSET, FIXED ASSET, CURRENT LIABILITY, LONG TERM LIABILITY,
     * DIRECT EXPENSE, INDIRECT EXPENSE, OPERATING INCOME, OTHER INCOME, CAPITAL.
     * Matching is done on case-insensitive substrings so it tolerates variants.
     */
    public static ColorHint colorForCategory(String accountCategory) {
        String c = normalize(accountCategory);

        // Asset categories
        if (c.contains("CURRENT") && c.contains("ASSET")) {
            return ColorHint.SUCCESS;
        }
        if (c.contains("FIXED") && c.contains("ASSET")) {
            return ColorHint.INFO;
        }

        // Liability categories
        if (c.contains("CURRENT") && c.contains("LIAB")) {
            return ColorHint.DANGER;
        }
        if (c.contains("LONG") && c.contains("LIAB")) {
            return ColorHint.WARNING;
        }

        // Income categories — OPERATING is primary revenue; OTHER is supplementary
        if (c.contains("OPERATING") && c.contains("INCOME")) {
            return ColorHint.SUCCESS;
        }
        if (c.contains("OTHER") && c.contains("INCOME")) {
            return ColorHint.INFO;
        }

        // Expense categories — DIRECT costs are more critical than INDIRECT overheads
        if (c.contains("DIRECT") && c.contains("EXPENSE")) {
            return ColorHint.DANGER;
        }
        if (c.contains("INDIRECT") && c.contains("EXPENSE")) {
            return ColorHint.WARNING;
        }

        // Equity / Capital
        if (c.contains("CAPITAL") || c.contains("EQUIT")) {
            return ColorHint.PRIMARY;
        }

        // Broad fall-backs for non-standard category names
        if (c.contains("LIAB")) {
            return ColorHint.WARNING;
        }
        if (c.contains("ASSET")) {
            return ColorHint.PRIMARY;
        }
        if (c.contains("INCOME") || c.contains("REVENUE")) {
            return ColorHint.SUCCESS;
        }
        if (c.contains("EXPENSE") || c.contains("COST")) {
            return ColorHint.DANGER;
        }
        return ColorHint.NEUTRAL;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toUpperCase();
    }
}
