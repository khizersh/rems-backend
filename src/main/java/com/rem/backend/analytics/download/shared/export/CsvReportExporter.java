package com.rem.backend.analytics.download.shared.export;

import com.rem.backend.analytics.download.shared.dto.FinancialStatementReport;
import com.rem.backend.analytics.download.shared.dto.ReportLine;
import com.rem.backend.analytics.download.shared.dto.ReportSection;
import com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Converts a {@link FinancialStatementReport} into a CSV byte array for file download.
 */
public final class CsvReportExporter {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private CsvReportExporter() {
    }

    public static byte[] toCsv(FinancialStatementReport report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            pw.print('\ufeff'); // UTF-8 BOM for Excel
            writeMetadata(pw, report);

            String key = report.getReportKey() == null ? "" : report.getReportKey();
            switch (key) {
                case "trial-balance" -> writeTrialBalance(pw, report);
                case "profit-loss" -> writeProfitLoss(pw, report);
                case "balance-sheet" -> writeBalanceSheet(pw, report);
                case "general-ledger", "account-statement" -> writeGeneralLedger(pw, report);
                case "journal-register" -> writeJournalRegister(pw, report);
                default -> writeGeneric(pw, report);
            }
        }
        return out.toByteArray();
    }

    public static String filename(FinancialStatementReport report) {
        String base = report.getReportKey() != null ? report.getReportKey() : "report";
        String datePart = report.getGeneratedAt() != null
                ? report.getGeneratedAt().toLocalDate().toString()
                : "export";
        return base + "-" + datePart + ".csv";
    }

    private static void writeMetadata(PrintWriter pw, FinancialStatementReport report) {
        pw.println("Report," + esc(report.getTitle()));
        if (report.getOrganizationName() != null) {
            pw.println("Organization," + esc(report.getOrganizationName()));
        }
        if (report.getSubtitle() != null) {
            pw.println("Period," + esc(report.getSubtitle()));
        }
        if (report.getGeneratedAt() != null) {
            pw.println("Generated At," + esc(report.getGeneratedAt().format(DT)));
        }
        pw.println();
    }

    private static void writeTrialBalance(PrintWriter pw, FinancialStatementReport report) {
        pw.println("Account Code,Account Name,Account Type,Opening,Debit,Credit,Closing");
        for (ReportSection section : safeSections(report)) {
            for (ReportLine line : safeLines(section)) {
                pw.printf("%s,%s,%s,%s,%s,%s,%s%n",
                        esc(line.getAccountCode()),
                        esc(line.getAccountName()),
                        esc(line.getAccountType()),
                        fmt(line.getOpeningBalance()),
                        fmt(line.getDebit()),
                        fmt(line.getCredit()),
                        fmt(line.getClosingBalance()));
            }
        }
        if (report.getTotals() != null) {
            pw.println();
            pw.printf("TOTALS,,,%s,%s,%s,%s%n",
                    fmt(report.getTotals().getTotalOpening()),
                    fmt(report.getTotals().getTotalDebit()),
                    fmt(report.getTotals().getTotalCredit()),
                    fmt(report.getTotals().getTotalClosing()));
        }
    }

    private static void writeProfitLoss(PrintWriter pw, FinancialStatementReport report) {
        pw.println("Section,Category,Account Code,Account Name,Amount");
        for (ReportSection section : safeSections(report)) {
            for (ReportLine line : safeLines(section)) {
                pw.printf("%s,%s,%s,%s,%s%n",
                        esc(section.getLabel()),
                        esc(line.getAccountCategory()),
                        esc(line.getAccountCode()),
                        esc(line.getAccountName()),
                        fmt(line.getAmount()));
            }
            pw.printf("%s,,,Section Total,%s%n", esc(section.getLabel()), fmt(section.getSectionTotal()));
            pw.println();
        }
        if (report.getTotals() != null) {
            pw.printf("Net Profit,,,,%s%n", fmt(report.getTotals().getNetProfit()));
        }
    }

    private static void writeBalanceSheet(PrintWriter pw, FinancialStatementReport report) {
        pw.println("Section,Category,Account Code,Account Name,Balance");
        for (ReportSection section : safeSections(report)) {
            for (ReportLine line : safeLines(section)) {
                pw.printf("%s,%s,%s,%s,%s%n",
                        esc(section.getLabel()),
                        esc(line.getAccountCategory()),
                        esc(line.getAccountCode()),
                        esc(line.getAccountName()),
                        fmt(line.getAmount()));
            }
            pw.printf("%s,,,Section Total,%s%n", esc(section.getLabel()), fmt(section.getSectionTotal()));
            pw.println();
        }
    }

    private static void writeGeneralLedger(PrintWriter pw, FinancialStatementReport report) {
        pw.println("Date,Journal ID,Reference,Account Code,Account Name,Description,Debit,Credit,Running Balance");
        for (ReportSection section : safeSections(report)) {
            pw.println();
            pw.println("Account," + esc(section.getLabel()));
            for (ReportLine line : safeLines(section)) {
                String date = line.getTransactionDate() != null
                        ? line.getTransactionDate().format(DT) : "";
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        esc(date),
                        line.getJournalEntryId() != null ? line.getJournalEntryId().toString() : "",
                        esc(line.getReferenceType()),
                        esc(line.getAccountCode()),
                        esc(line.getAccountName()),
                        esc(line.getDescription()),
                        fmt(line.getDebit()),
                        fmt(line.getCredit()),
                        fmt(line.getRunningBalance()));
            }
        }
    }

    private static void writeJournalRegister(PrintWriter pw, FinancialStatementReport report) {
        pw.println("Date,Journal ID,Reference,Journal Description,Account Code,Account Name,Line Description,Debit,Credit");
        for (ReportSection section : safeSections(report)) {
            for (ReportLine line : safeLines(section)) {
                String date = line.getTransactionDate() != null
                        ? line.getTransactionDate().format(DT) : "";
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        esc(date),
                        line.getJournalEntryId() != null ? line.getJournalEntryId().toString() : "",
                        esc(line.getReferenceType()),
                        esc(line.getJournalDescription()),
                        esc(line.getAccountCode()),
                        esc(line.getAccountName()),
                        esc(line.getDescription()),
                        fmt(line.getDebit()),
                        fmt(line.getCredit()));
            }
        }
    }

    private static void writeGeneric(PrintWriter pw, FinancialStatementReport report) {
        pw.println("Section,Account Code,Account Name,Amount,Debit,Credit,Balance");
        for (ReportSection section : safeSections(report)) {
            for (ReportLine line : safeLines(section)) {
                pw.printf("%s,%s,%s,%s,%s,%s,%s%n",
                        esc(section.getLabel()),
                        esc(line.getAccountCode()),
                        esc(line.getAccountName()),
                        fmt(line.getAmount()),
                        fmt(line.getDebit()),
                        fmt(line.getCredit()),
                        fmt(line.getClosingBalance()));
            }
        }
    }

    private static List<ReportSection> safeSections(FinancialStatementReport report) {
        return report.getSections() != null ? report.getSections() : List.of();
    }

    private static List<ReportLine> safeLines(ReportSection section) {
        return section.getLines() != null ? section.getLines() : List.of();
    }

    private static String fmt(Double value) {
        if (value == null) {
            return "0.00";
        }
        return String.format("%.2f", ReportingFormatUtil.round2(value));
    }

    private static String esc(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
