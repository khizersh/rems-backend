package com.rem.backend.analytics.download.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportLine {
    private String accountCode;
    private String accountName;
    private String accountType;
    private String accountCategory;
    private String accountGroup;
    private Double openingBalance;
    private Double debit;
    private Double credit;
    private Double closingBalance;
    private Double amount;
    private Double runningBalance;
    private int indentLevel;

    // Journal register / ledger context
    private Long journalEntryId;
    private LocalDateTime transactionDate;
    private String referenceType;
    private String description;
    private String journalDescription;
}
