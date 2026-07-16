package com.rem.backend.analytics.reporting.admin.service;

import com.rem.backend.accountingmanagement.utility.JournalUtilities;
import com.rem.backend.analytics.reporting.admin.query.AdminInventoryReportRepository;
import com.rem.backend.analytics.reporting.admin.query.AdminPropertyReportRepository;
import com.rem.backend.analytics.reporting.shared.dto.ChartData;
import com.rem.backend.analytics.reporting.shared.dto.ChartSeries;
import com.rem.backend.analytics.reporting.shared.dto.GroupedSummary;
import com.rem.backend.analytics.reporting.shared.dto.KpiCard;
import com.rem.backend.analytics.reporting.shared.dto.ReportingView;
import com.rem.backend.analytics.reporting.shared.enums.ChartType;
import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;
import com.rem.backend.analytics.reporting.shared.util.DateRange;
import com.rem.backend.analytics.reporting.shared.util.ReportingDateUtil;
import com.rem.backend.analytics.reporting.shared.util.ReportingFactory;
import com.rem.backend.projectmanagement.repository.ProjectRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil.nz;

/**
 * Admin reporting for projects, property assets and sellable inventory.
 * Inventory valuation is taken from unit prices; the accounting property-inventory account
 * is shown alongside as the capitalised book value.
 */
@Service
@AllArgsConstructor
public class AdminProjectPropertyReportService {

    private final ProjectRepo projectRepo;
    private final AdminInventoryReportRepository inventoryReportRepo;
    private final AdminPropertyReportRepository propertyReportRepo;
    private final AdminFinancialReportService financialReportService;

    public ReportingView projectsOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        long totalProjects = projectRepo.countByOrganizationId(organizationId);
        double constructionInventory = financialReportService.codeBalance(
                organizationId, range, JournalUtilities.CONSTRUCTION_INVENTORY);
        double propertyInventory = financialReportService.codeBalance(
                organizationId, range, JournalUtilities.PROPERTY_INVENTORY);

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("totalProjects", "Total Projects", totalProjects, "FaDiagramProject", ColorHint.PRIMARY),
                ReportingFactory.currencyCard("constructionInventory", "Construction Inventory", constructionInventory, "FaTrowelBricks", ColorHint.INFO),
                ReportingFactory.currencyCard("propertyInventory", "Property Inventory (Book)", propertyInventory, "FaLandmark", ColorHint.WARNING)
        );

        // TODO(project-progress): there is no stored project progress %. A meaningful progress
        // metric would compare collected vs contracted value (analytics module already computes
        // received vs Project.totalAmount). Left out here to avoid a misleading derived number.

        return ReportingView.builder()
                .key("projects-overview").title("Projects Overview")
                .subtitle("Project portfolio & inventory book value")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of())
                .charts(List.of())
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    public ReportingView propertyOverview(long organizationId, ReportingFilter filter) {
        Map<String, Object> snap = propertyReportRepo.propertyAssetSnapshot(organizationId);
        double total = num(snap, "totalAssets");
        double available = num(snap, "availableAssets");
        double used = num(snap, "usedAssets");
        double totalValue = num(snap, "totalAcquiredValue");
        double availableValue = num(snap, "availableValue");

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("totalAssets", "Total Property Assets", total, "FaLandmark", ColorHint.PRIMARY),
                ReportingFactory.countCard("availableAssets", "Available Assets", available, "FaSquareCheck", ColorHint.SUCCESS),
                ReportingFactory.countCard("usedAssets", "Used Assets", used, "FaSquareMinus", ColorHint.NEUTRAL),
                ReportingFactory.currencyCard("totalAcquiredValue", "Total Acquired Value", totalValue, "FaSackDollar", ColorHint.INFO),
                ReportingFactory.currencyCard("availableValue", "Available Asset Value", availableValue, "FaCoins", ColorHint.WARNING)
        );

        GroupedSummary statusBreakdown = GroupedSummary.builder()
                .key("propertyAssetStatus").label("Property Asset Status")
                .total(total)
                .items(List.of(
                        ReportingFactory.countValue("available", "Available", available, ColorHint.SUCCESS),
                        ReportingFactory.countValue("used", "Used", used, ColorHint.NEUTRAL)))
                .build();

        return ReportingView.builder()
                .key("property-overview").title("Property Overview")
                .subtitle("Acquired property assets")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(statusBreakdown))
                .charts(List.of(pie("propertyAssetDistribution", "Property Asset Distribution",
                        List.of("Available", "Used"), List.of(available, used),
                        List.of(ColorHint.SUCCESS, ColorHint.NEUTRAL))))
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    public ReportingView inventoryOverview(long organizationId, ReportingFilter filter) {
        Map<String, Object> snap = inventoryReportRepo.inventorySnapshot(organizationId);
        double totalUnits = num(snap, "totalUnits");
        double available = num(snap, "availableUnits");
        double booked = num(snap, "bookedUnits");
        double totalValuation = num(snap, "totalValuation");
        double availableValuation = num(snap, "availableValuation");

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("totalUnits", "Total Units", totalUnits, "FaBuilding", ColorHint.PRIMARY),
                ReportingFactory.countCard("availableUnits", "Available Units", available, "FaDoorOpen", ColorHint.SUCCESS),
                ReportingFactory.countCard("bookedUnits", "Booked / Reserved Units", booked, "FaDoorClosed", ColorHint.WARNING),
                ReportingFactory.currencyCard("totalValuation", "Total Inventory Value", totalValuation, "FaSackDollar", ColorHint.INFO),
                ReportingFactory.currencyCard("availableValuation", "Available Inventory Value", availableValuation, "FaCoins", ColorHint.SUCCESS)
        );

        GroupedSummary breakdown = GroupedSummary.builder()
                .key("unitStatus").label("Unit Availability")
                .total(totalUnits)
                .items(List.of(
                        ReportingFactory.countValue("available", "Available", available, ColorHint.SUCCESS),
                        ReportingFactory.countValue("booked", "Booked / Reserved", booked, ColorHint.WARNING)))
                .build();

        return ReportingView.builder()
                .key("inventory-overview").title("Inventory Overview")
                .subtitle("Sellable unit inventory")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(breakdown))
                .charts(List.of(pie("unitDistribution", "Unit Distribution",
                        List.of("Available", "Booked"), List.of(available, booked),
                        List.of(ColorHint.SUCCESS, ColorHint.WARNING))))
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    private static ChartData pie(String key, String title, List<String> labels,
                                 List<Double> values, List<ColorHint> colors) {
        return ChartData.builder()
                .key(key).title(title).type(ChartType.DONUT)
                .labels(labels)
                .series(List.of(ChartSeries.builder().name("Count")
                        .data(new java.util.ArrayList<Object>(values)).build()))
                .build();
    }

    private static double num(Map<String, Object> row, String key) {
        if (row == null) {
            return 0d;
        }
        return nz((Number) row.get(key));
    }
}
