package com.rem.backend.analytics.reporting.admin.service;

import com.rem.backend.accountingmanagement.utility.JournalUtilities;
import com.rem.backend.analytics.reporting.shared.dto.ChartData;
import com.rem.backend.analytics.reporting.shared.dto.ChartSeries;
import com.rem.backend.analytics.reporting.shared.dto.GroupedSummary;
import com.rem.backend.analytics.reporting.shared.dto.KpiCard;
import com.rem.backend.analytics.reporting.shared.dto.LabeledValue;
import com.rem.backend.analytics.reporting.shared.dto.ReportingView;
import com.rem.backend.analytics.reporting.shared.enums.ChartType;
import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;
import com.rem.backend.analytics.reporting.shared.util.DateRange;
import com.rem.backend.analytics.reporting.shared.util.ReportingDateUtil;
import com.rem.backend.analytics.reporting.shared.util.ReportingFactory;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrder;
import com.rem.backend.purchasemanagement.enums.PoStatus;
import com.rem.backend.purchasemanagement.repository.ExpenseRepo;
import com.rem.backend.purchasemanagement.repository.PurchaseOrderRepo;
import com.rem.backend.vendormanagement.repository.VendorAccountRepo;
import com.rem.backend.warehousemanagement.entity.Stock;
import com.rem.backend.warehousemanagement.entity.Warehouse;
import com.rem.backend.warehousemanagement.repo.StockRepository;
import com.rem.backend.warehousemanagement.repo.WarehouseRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil.nz;

/**
 * Admin reporting for procurement (purchase orders + expenses), vendors and warehouses.
 * Vendor payable and accounting figures come from the GL; counts/volumes from business tables.
 */
@Service
@AllArgsConstructor
public class AdminProcurementReportService {

    private final PurchaseOrderRepo purchaseOrderRepo;
    private final ExpenseRepo expenseRepo;
    private final VendorAccountRepo vendorAccountRepo;
    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final AdminFinancialReportService financialReportService;

    public ReportingView purchaseOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);

        long open = 0, partial = 0, closed = 0, cancelled = 0;
        double poVolume = 0d;
        long totalPo = 0;
        for (PoStatus status : PoStatus.values()) {
            List<PurchaseOrder> pos = purchaseOrderRepo.findByOrgIdAndStatus(organizationId, status);
            long count = pos.size();
            totalPo += count;
            double sum = pos.stream().mapToDouble(p -> nz(p.getTotalAmount())).sum();
            poVolume += sum;
            switch (status) {
                case OPEN -> open = count;
                case PARTIAL -> partial = count;
                case CLOSED -> closed = count;
                case CANCELLED -> cancelled = count;
            }
        }

        // Expense roll-up for the period: [totalAmount, amountPaid, creditAmount, count]
        double expenseTotal = 0, expensePaid = 0, expenseCredit = 0;
        List<Object[]> expenseAgg = expenseRepo.summarizeByOrgAndDateRange(organizationId, range.getStart(), range.getEnd());
        if (expenseAgg != null && !expenseAgg.isEmpty()) {
            Object[] row = expenseAgg.get(0);
            expenseTotal = num(row, 0);
            expensePaid = num(row, 1);
            expenseCredit = num(row, 2);
        }

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("totalPurchaseOrders", "Total Purchase Orders", totalPo, "FaClipboardList", ColorHint.PRIMARY),
                ReportingFactory.countCard("openPurchaseOrders", "Open POs", open, "FaFolderOpen", ColorHint.WARNING),
                ReportingFactory.currencyCard("purchaseVolume", "Purchase Order Volume", poVolume, "FaTruckRampBox", ColorHint.INFO),
                ReportingFactory.currencyCard("expenseTotalPeriod", "Expenses (Period)", expenseTotal, "FaReceipt", ColorHint.DANGER),
                ReportingFactory.currencyCard("expensePaidPeriod", "Expenses Paid (Period)", expensePaid, "FaCircleCheck", ColorHint.SUCCESS),
                ReportingFactory.currencyCard("expenseCreditPeriod", "Expenses on Credit (Period)", expenseCredit, "FaClock", ColorHint.WARNING)
        );

        GroupedSummary poStatus = GroupedSummary.builder()
                .key("poStatus").label("Purchase Order Status")
                .total(totalPo)
                .items(List.of(
                        ReportingFactory.countValue("open", "Open", open, ColorHint.WARNING),
                        ReportingFactory.countValue("partial", "Partial", partial, ColorHint.INFO),
                        ReportingFactory.countValue("closed", "Closed", closed, ColorHint.SUCCESS),
                        ReportingFactory.countValue("cancelled", "Cancelled", cancelled, ColorHint.DANGER)))
                .build();

        ChartData statusChart = ChartData.builder()
                .key("poStatusDistribution").title("PO Status Distribution").type(ChartType.DONUT)
                .labels(List.of("Open", "Partial", "Closed", "Cancelled"))
                .series(List.of(ChartSeries.builder().name("Purchase Orders")
                        .data(List.of(open, partial, closed, cancelled)).build()))
                .build();

        return ReportingView.builder()
                .key("purchase-overview").title("Purchase Overview")
                .subtitle("Procurement activity")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(poStatus))
                .charts(List.of(statusChart))
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    public ReportingView vendorOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        long vendorCount = vendorAccountRepo.findAllByOrgId(organizationId).size();
        double payableLedger = financialReportService.codeBalance(organizationId, range, JournalUtilities.VENDOR_PAYABLE);
        double payableBusiness = vendorAccountRepo.findTotalPayableByOrgId(organizationId);

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("totalVendors", "Total Vendors", vendorCount, "FaPeopleCarryBox", ColorHint.PRIMARY),
                ReportingFactory.currencyCard("vendorPayableLedger", "Vendor Payable (Accounting)", payableLedger, "FaFileInvoiceDollar", ColorHint.DANGER),
                ReportingFactory.currencyCard("vendorPayableBusiness", "Vendor Payable (Sub-ledger)", payableBusiness, "FaMoneyCheckDollar", ColorHint.WARNING)
        );

        return ReportingView.builder()
                .key("vendor-overview").title("Vendor Overview")
                .subtitle("Vendor base & payables")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of())
                .charts(List.of())
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    public ReportingView warehouseOverview(long organizationId, ReportingFilter filter) {
        List<Warehouse> warehouses = warehouseRepository.findByOrganizationIdAndActiveTrue(organizationId);

        double totalQuantity = 0d;
        double totalValuation = 0d;
        long distinctStockRows = 0;
        List<LabeledValue> perWarehouse = new ArrayList<>();
        for (Warehouse w : warehouses) {
            List<Stock> stocks = stockRepository.findByWarehouseId(w.getId());
            double whQty = 0d;
            double whVal = 0d;
            for (Stock s : stocks) {
                double qty = bd(s.getQuantity());
                double rate = bd(s.getAvgRate());
                whQty += qty;
                whVal += qty * rate;
            }
            distinctStockRows += stocks.size();
            totalQuantity += whQty;
            totalValuation += whVal;
            perWarehouse.add(ReportingFactory.currencyValue(
                    "wh_" + w.getId(), warehouseLabel(w), whVal, ColorHint.INFO));
        }

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("totalWarehouses", "Active Warehouses", warehouses.size(), "FaWarehouse", ColorHint.PRIMARY),
                ReportingFactory.countCard("stockLines", "Stock Item Lines", distinctStockRows, "FaBoxesStacked", ColorHint.INFO),
                ReportingFactory.countCard("totalQuantity", "Total Stock Quantity", totalQuantity, "FaCubes", ColorHint.NEUTRAL),
                ReportingFactory.currencyCard("stockValuation", "Stock Valuation", totalValuation, "FaSackDollar", ColorHint.SUCCESS)
        );

        GroupedSummary byWarehouse = GroupedSummary.builder()
                .key("warehouseValuation").label("Valuation by Warehouse")
                .total(totalValuation)
                .items(perWarehouse)
                .build();

        // TODO(low-stock): Stock has no reorder-level field, so reorder-based low-stock alerts
        // cannot be derived reliably here. Add a reorder threshold to Stock (or Item) to enable.

        return ReportingView.builder()
                .key("warehouse-overview").title("Warehouse Overview")
                .subtitle("Inventory stock position")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(byWarehouse))
                .charts(List.of())
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    private static String warehouseLabel(Warehouse w) {
        if (w.getName() != null && !w.getName().isBlank()) {
            return w.getName();
        }
        return "Warehouse #" + w.getId();
    }

    private static double bd(BigDecimal value) {
        return value == null ? 0d : value.doubleValue();
    }

    private static double num(Object[] row, int idx) {
        if (row == null || idx >= row.length || row[idx] == null) {
            return 0d;
        }
        return ((Number) row[idx]).doubleValue();
    }
}
