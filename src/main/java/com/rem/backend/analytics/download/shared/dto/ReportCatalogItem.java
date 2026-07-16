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
public class ReportCatalogItem {
    private String key;
    private String title;
    private String description;
    private List<String> supportedFormats;
    private String endpoint;
}
