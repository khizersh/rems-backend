package com.rem.backend.warehousemanagement.service;

import com.rem.backend.warehousemanagement.entity.Stock;
import com.rem.backend.warehousemanagement.entity.StockLedger;
import com.rem.backend.warehousemanagement.entity.Warehouse;
import com.rem.backend.enums.StockRefType;
import com.rem.backend.warehousemanagement.repo.StockRepository;
import com.rem.backend.warehousemanagement.repo.StockLedgerRepository;
import com.rem.backend.warehousemanagement.repo.WarehouseRepository;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final StockRepository stockRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Add stock to warehouse with rate calculation
     */
    @Transactional
    public void addStock(Long warehouseId, Long itemId, BigDecimal quantity, BigDecimal rate,
                        StockRefType refType, Long refId, String remarks, String loggedInUser) {

        validateAddStockParams(warehouseId, itemId, quantity, rate, refType, refId, loggedInUser);

        // Validate warehouse exists and is active
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new IllegalArgumentException("Warehouse not found with ID: " + warehouseId));

        if (!warehouse.getActive()) {
            throw new IllegalArgumentException("Cannot add stock to inactive warehouse");
        }

        // Find existing stock or create new
        Stock stock = stockRepository.findByWarehouseIdAndItemId(warehouseId, itemId)
            .orElse(createNewStock(warehouseId, itemId, loggedInUser));

        // Calculate new average rate using weighted average
        BigDecimal currentValue = stock.getQuantity().multiply(stock.getAvgRate());
        BigDecimal addedValue = quantity.multiply(rate);
        BigDecimal newQuantity = stock.getQuantity().add(quantity);

        BigDecimal newAvgRate = BigDecimal.ZERO;
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            newAvgRate = currentValue.add(addedValue).divide(newQuantity, 4, RoundingMode.HALF_UP);
        }

        // Update stock
        stock.setQuantity(newQuantity);
        stock.setAvgRate(newAvgRate);
        stock.setUpdatedBy(loggedInUser);
        stock = stockRepository.save(stock);

        // Create stock ledger entry
        createStockLedgerEntry(warehouseId, itemId, refType, refId, quantity, BigDecimal.ZERO,
                              stock.getQuantity(), rate, quantity.multiply(rate), remarks, loggedInUser);

        log.info("Stock added: Warehouse={}, Item={}, Qty={}, Rate={}, NewBalance={}",
                warehouseId, itemId, quantity, rate, stock.getQuantity());
    }

    /**
     * Deduct stock from warehouse with negative stock prevention
     */
    @Transactional
    public void deductStock(Long warehouseId, Long itemId, BigDecimal quantity,
                           StockRefType refType, Long refId, String remarks, String loggedInUser) {

        validateDeductStockParams(warehouseId, itemId, quantity, refType, refId, loggedInUser);

        // Find existing stock
        Stock stock = stockRepository.findByWarehouseIdAndItemId(warehouseId, itemId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No stock available for item " + itemId + " in warehouse " + warehouseId));

        // Check sufficient quantity available
        BigDecimal availableQty = stock.getQuantity().subtract(stock.getReservedQuantity());
        if (availableQty.compareTo(quantity) < 0) {
            throw new IllegalArgumentException(
                String.format("Insufficient stock. Available: %s, Requested: %s", availableQty, quantity));
        }

        // Update stock quantity
        BigDecimal newQuantity = stock.getQuantity().subtract(quantity);
        stock.setQuantity(newQuantity);
        stock.setUpdatedBy(loggedInUser);
        stock = stockRepository.save(stock);

        // Create stock ledger entry
        createStockLedgerEntry(warehouseId, itemId, refType, refId, BigDecimal.ZERO, quantity,
                              stock.getQuantity(), stock.getAvgRate(),
                              quantity.multiply(stock.getAvgRate()), remarks, loggedInUser);

        log.info("Stock deducted: Warehouse={}, Item={}, Qty={}, NewBalance={}",
                warehouseId, itemId, quantity, stock.getQuantity());
    }

    /**
     * Transfer stock between warehouses
     */
    @Transactional
    public void transferStock(Long fromWarehouseId, Long toWarehouseId, Long itemId,
                             BigDecimal quantity, Long refId, String remarks, String loggedInUser) {

        if (fromWarehouseId.equals(toWarehouseId)) {
            throw new IllegalArgumentException("Source and destination warehouses cannot be the same");
        }

        // Get source stock for rate calculation
        Stock sourceStock = stockRepository.findByWarehouseIdAndItemId(fromWarehouseId, itemId)
            .orElseThrow(() -> new IllegalArgumentException("No stock found in source warehouse"));

        BigDecimal transferRate = sourceStock.getAvgRate();

        // Deduct from source warehouse
        deductStock(fromWarehouseId, itemId, quantity, StockRefType.TRANSFER, refId,
                   "Transfer OUT to warehouse " + toWarehouseId + " - " + remarks, loggedInUser);

        // Add to destination warehouse
        addStock(toWarehouseId, itemId, quantity, transferRate, StockRefType.TRANSFER, refId,
                "Transfer IN from warehouse " + fromWarehouseId + " - " + remarks, loggedInUser);

        log.info("Stock transferred: From={}, To={}, Item={}, Qty={}",
                fromWarehouseId, toWarehouseId, itemId, quantity);
    }

    /**
     * Adjust stock (positive or negative adjustment)
     */
    @Transactional
    public void adjustStock(Long warehouseId, Long itemId, BigDecimal quantity,
                           Boolean increase, String remarks, String loggedInUser) {

        validateAdjustStockParams(warehouseId, itemId, quantity, increase, loggedInUser);

        if (increase) {
            // For stock increase, we need a rate - use existing rate or zero
            Stock existingStock = stockRepository.findByWarehouseIdAndItemId(warehouseId, itemId)
                .orElse(null);
            BigDecimal rate = (existingStock != null) ? existingStock.getAvgRate() : BigDecimal.ZERO;

            addStock(warehouseId, itemId, quantity, rate, StockRefType.ADJUSTMENT,
                    System.currentTimeMillis(), "Stock increase adjustment: " + remarks, loggedInUser);
        } else {
            deductStock(warehouseId, itemId, quantity, StockRefType.ADJUSTMENT,
                       System.currentTimeMillis(), "Stock decrease adjustment: " + remarks, loggedInUser);
        }

        log.info("Stock adjusted: Warehouse={}, Item={}, Qty={}, Increase={}",
                warehouseId, itemId, quantity, increase);
    }

    /**
     * Reserve stock for future allocation
     */
    @Transactional
    public void reserveStock(Long warehouseId, Long itemId, BigDecimal quantity, String loggedInUser) {
        Stock stock = stockRepository.findByWarehouseIdAndItemId(warehouseId, itemId)
            .orElseThrow(() -> new IllegalArgumentException("Stock not found"));

        BigDecimal availableQty = stock.getQuantity().subtract(stock.getReservedQuantity());
        if (availableQty.compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Insufficient stock available for reservation");
        }

        stock.setReservedQuantity(stock.getReservedQuantity().add(quantity));
        stock.setUpdatedBy(loggedInUser);
        stockRepository.save(stock);

        log.info("Stock reserved: Warehouse={}, Item={}, Qty={}", warehouseId, itemId, quantity);
    }

    /**
     * Release reserved stock
     */
    @Transactional
    public void releaseReservedStock(Long warehouseId, Long itemId, BigDecimal quantity, String loggedInUser) {
        Stock stock = stockRepository.findByWarehouseIdAndItemId(warehouseId, itemId)
            .orElseThrow(() -> new IllegalArgumentException("Stock not found"));

        if (stock.getReservedQuantity().compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Cannot release more than reserved quantity");
        }

        stock.setReservedQuantity(stock.getReservedQuantity().subtract(quantity));
        stock.setUpdatedBy(loggedInUser);
        stockRepository.save(stock);

        log.info("Reserved stock released: Warehouse={}, Item={}, Qty={}", warehouseId, itemId, quantity);
    }

    // ====================== PRIVATE HELPER METHODS ======================

    private Stock createNewStock(Long warehouseId, Long itemId, String loggedInUser) {
        Stock stock = new Stock();
        stock.setWarehouseId(warehouseId);
        stock.setItemId(itemId);
        stock.setQuantity(BigDecimal.ZERO);
        stock.setReservedQuantity(BigDecimal.ZERO);
        stock.setAvgRate(BigDecimal.ZERO);
        stock.setCreatedBy(loggedInUser);
        stock.setUpdatedBy(loggedInUser);
        return stock;
    }

    private void createStockLedgerEntry(Long warehouseId, Long itemId, StockRefType refType, Long refId,
                                       BigDecimal qtyIn, BigDecimal qtyOut, BigDecimal balanceAfter,
                                       BigDecimal rate, BigDecimal amount, String remarks, String loggedInUser) {

        StockLedger ledger = new StockLedger();
        ledger.setWarehouseId(warehouseId);
        ledger.setItemId(itemId);
        ledger.setRefType(refType);
        ledger.setRefId(refId);
        ledger.setTxnDate(LocalDateTime.now());
        ledger.setQtyIn(qtyIn);
        ledger.setQtyOut(qtyOut);
        ledger.setBalanceAfter(balanceAfter);
        ledger.setRate(rate);
        ledger.setAmount(amount);
        ledger.setRemarks(remarks);
        ledger.setCreatedBy(loggedInUser);
        ledger.setUpdatedBy(loggedInUser);

        stockLedgerRepository.save(ledger);
    }

    // ====================== VALIDATION METHODS ======================

    private void validateAddStockParams(Long warehouseId, Long itemId, BigDecimal quantity,
                                       BigDecimal rate, StockRefType refType, Long refId, String loggedInUser) {
        ValidationService.validate(warehouseId, "Warehouse ID");
        ValidationService.validate(itemId, "Item ID");
        ValidationService.validate(quantity, "Quantity");
        ValidationService.validate(rate, "Rate");
        ValidationService.validate(refType, "Reference Type");
        ValidationService.validate(refId, "Reference ID");
        ValidationService.validate(loggedInUser, "Logged in user");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rate cannot be negative");
        }
    }

    private void validateDeductStockParams(Long warehouseId, Long itemId, BigDecimal quantity,
                                          StockRefType refType, Long refId, String loggedInUser) {
        ValidationService.validate(warehouseId, "Warehouse ID");
        ValidationService.validate(itemId, "Item ID");
        ValidationService.validate(quantity, "Quantity");
        ValidationService.validate(refType, "Reference Type");
        ValidationService.validate(refId, "Reference ID");
        ValidationService.validate(loggedInUser, "Logged in user");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    private void validateAdjustStockParams(Long warehouseId, Long itemId, BigDecimal quantity,
                                          Boolean increase, String loggedInUser) {
        ValidationService.validate(warehouseId, "Warehouse ID");
        ValidationService.validate(itemId, "Item ID");
        ValidationService.validate(quantity, "Quantity");
        ValidationService.validate(increase, "Increase flag");
        ValidationService.validate(loggedInUser, "Logged in user");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }
}
