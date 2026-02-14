package com.rem.backend.warehousemanagement.service;

import com.rem.backend.warehousemanagement.dto.ExpenseItemRequestDTO;
import com.rem.backend.warehousemanagement.entity.ExpenseItem;
import com.rem.backend.enums.ReceiptType;
import com.rem.backend.enums.StockRefType;
import com.rem.backend.purchasemanagement.entity.grn.Grn;
import com.rem.backend.purchasemanagement.entity.grn.GrnItems;
import com.rem.backend.warehousemanagement.repo.ExpenseItemRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseIntegrationService {

    private final InventoryService inventoryService;
    private final ExpenseItemRepository expenseItemRepository;

    /**
     * Process GRN approval and add stock to warehouse if receipt type is WAREHOUSE_STOCK
     */
    @Transactional
    public void processGrnApproval(Grn grn, List<GrnItems> grnItems, String loggedInUser) {
        try {
            if (grn.getReceiptType() == ReceiptType.WAREHOUSE_STOCK && grn.getWarehouseId() != null) {

                for (GrnItems grnItem : grnItems) {
                    // For GRN, we might not have rate info in GrnItems, use zero or get from PO
                    BigDecimal rate = BigDecimal.ZERO; // You might want to fetch this from PO items

                    inventoryService.addStock(
                        grn.getWarehouseId(),
                        grnItem.getItemId(),
                        BigDecimal.valueOf(grnItem.getQuantityReceived()),
                        rate,
                        StockRefType.GRN,
                        grnItem.getId(),
                        "Stock received from GRN: " + grn.getGrnNumber(),
                        loggedInUser
                    );
                }

                log.info("GRN processed for warehouse stock: GRN={}, Warehouse={}", grn.getId(), grn.getWarehouseId());

            } else if (grn.getReceiptType() == ReceiptType.DIRECT_CONSUME) {
                // Direct consumption - no stock entry needed
                log.info("GRN processed for direct consumption: GRN={}, Project={}", grn.getId(), grn.getDirectConsumeProjectId());
            }

        } catch (Exception e) {
            log.error("Error processing GRN approval: {}", e.getMessage());
            throw new RuntimeException("Failed to process GRN approval: " + e.getMessage());
        }
    }

    /**
     * Process expense items and add stock if stockEffect is true
     */
    @Transactional
    public Map<String, Object> processExpenseItems(ExpenseItemRequestDTO request, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "Logged in user");
            validateExpenseItemRequest(request);

            // Delete existing expense items for this expense
            expenseItemRepository.deleteByExpenseId(request.getExpenseId());

            for (ExpenseItemRequestDTO.ExpenseItemDTO itemDto : request.getExpenseItems()) {
                // Create expense item record
                ExpenseItem expenseItem = new ExpenseItem();
                expenseItem.setExpenseId(request.getExpenseId());
                expenseItem.setItemId(itemDto.getItemId());
                expenseItem.setUnitId(itemDto.getUnitId());
                expenseItem.setQuantity(itemDto.getQuantity());
                expenseItem.setRate(itemDto.getRate());
                expenseItem.setAmount(itemDto.getAmount());
                expenseItem.setWarehouseId(itemDto.getWarehouseId());
                expenseItem.setStockEffect(itemDto.getStockEffect());
                expenseItem.setCreatedBy(loggedInUser);
                expenseItem.setUpdatedBy(loggedInUser);

                expenseItem = expenseItemRepository.save(expenseItem);

                // Add stock if stockEffect is true and warehouseId is provided
                if (itemDto.getStockEffect() && itemDto.getWarehouseId() != null) {
                    inventoryService.addStock(
                        itemDto.getWarehouseId(),
                        itemDto.getItemId(),
                        itemDto.getQuantity(),
                        itemDto.getRate(),
                        StockRefType.DIRECT_EXPENSE_PURCHASE,
                        expenseItem.getId(),
                        "Stock from direct expense purchase - Expense ID: " + request.getExpenseId(),
                        loggedInUser
                    );

                    log.info("Stock added from expense: Expense={}, Item={}, Warehouse={}, Qty={}",
                            request.getExpenseId(), itemDto.getItemId(), itemDto.getWarehouseId(), itemDto.getQuantity());
                }
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Expense items processed successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error processing expense items: {}", e.getMessage());
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Issue material from warehouse (deduct stock)
     */
    @Transactional
    public Map<String, Object> issueMaterial(Long warehouseId, Long itemId, BigDecimal quantity,
                                           Long projectId, String remarks, String loggedInUser) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");
            ValidationService.validate(itemId, "Item ID");
            ValidationService.validate(quantity, "Quantity");
            ValidationService.validate(loggedInUser, "Logged in user");

            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }

            // Use projectId as refId if provided, otherwise use timestamp
            Long refId = projectId != null ? projectId : System.currentTimeMillis();
            String materialIssueRemarks = "Material issue to project " + projectId +
                                        (remarks != null ? " - " + remarks : "");

            inventoryService.deductStock(
                warehouseId,
                itemId,
                quantity,
                StockRefType.MATERIAL_ISSUE,
                refId,
                materialIssueRemarks,
                loggedInUser
            );

            log.info("Material issued: Warehouse={}, Item={}, Qty={}, Project={}",
                    warehouseId, itemId, quantity, projectId);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Material issued successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error issuing material: {}", e.getMessage());
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get expense items by expense ID
     */
    public Map<String, Object> getExpenseItems(Long expenseId) {
        try {
            ValidationService.validate(expenseId, "Expense ID");

            List<ExpenseItem> expenseItems = expenseItemRepository.findByExpenseId(expenseId);

            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseItems);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get expense items that affect stock by warehouse
     */
    public Map<String, Object> getStockEffectItemsByWarehouse(Long warehouseId) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");

            List<ExpenseItem> expenseItems = expenseItemRepository.findStockEffectItemsByWarehouse(warehouseId);

            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseItems);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ====================== VALIDATION METHODS ======================

    private void validateExpenseItemRequest(ExpenseItemRequestDTO request) {
        ValidationService.validate(request.getExpenseId(), "Expense ID");

        if (request.getExpenseItems() == null || request.getExpenseItems().isEmpty()) {
            throw new IllegalArgumentException("Expense items list cannot be null or empty");
        }

        for (ExpenseItemRequestDTO.ExpenseItemDTO item : request.getExpenseItems()) {
            ValidationService.validate(item.getItemId(), "Item ID");
            ValidationService.validate(item.getUnitId(), "Unit ID");
            ValidationService.validate(item.getQuantity(), "Quantity");
            ValidationService.validate(item.getRate(), "Rate");
            ValidationService.validate(item.getAmount(), "Amount");
            ValidationService.validate(item.getStockEffect(), "Stock effect flag");

            if (item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
            if (item.getRate().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Rate cannot be negative");
            }
            if (item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }

            // If stockEffect is true, warehouseId must be provided
            if (item.getStockEffect() && item.getWarehouseId() == null) {
                throw new IllegalArgumentException("Warehouse ID is required when stock effect is true");
            }
        }
    }
}
