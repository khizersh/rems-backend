package com.rem.backend.analytics.download.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSection {
    private String key;
    private String label;
    private List<ReportLine> lines;
    private Double sectionTotal;
    private String formattedSectionTotal;
}
