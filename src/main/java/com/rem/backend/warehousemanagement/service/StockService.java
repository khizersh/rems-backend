package com.rem.backend.warehousemanagement.service;

import com.rem.backend.warehousemanagement.dto.StockAdjustmentRequestDTO;
import com.rem.backend.warehousemanagement.dto.StockSummaryDTO;
import com.rem.backend.warehousemanagement.dto.StockTransferRequestDTO;
import com.rem.backend.warehousemanagement.entity.Stock;
import com.rem.backend.warehousemanagement.repo.StockRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final InventoryService inventoryService;

    public Map<String, Object> getStockByWarehouse(Long warehouseId, Pageable pageable) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");

            Page<Stock> stocks = stockRepository.findByWarehouseId(warehouseId, pageable);

            return ResponseMapper.buildResponse(Responses.SUCCESS, stocks);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getStockByItem(Long itemId, Pageable pageable) {
        try {
            ValidationService.validate(itemId, "Item ID");

            Page<Stock> stocks = stockRepository.findByItemId(itemId, pageable);

            return ResponseMapper.buildResponse(Responses.SUCCESS, stocks);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getStockByWarehouseAndItem(Long warehouseId, Long itemId) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");
            ValidationService.validate(itemId, "Item ID");

            Stock stock = stockRepository.findByWarehouseIdAndItemId(warehouseId, itemId)
                .orElse(null);

            return ResponseMapper.buildResponse(Responses.SUCCESS, stock);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAvailableStockByWarehouse(Long warehouseId) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");

            List<Stock> stocks = stockRepository.findAvailableStockByWarehouse(warehouseId);

            return ResponseMapper.buildResponse(Responses.SUCCESS, stocks);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAvailableStockByItem(Long itemId) {
        try {
            ValidationService.validate(itemId, "Item ID");

            List<Stock> stocks = stockRepository.findAvailableStockByItem(itemId);

            return ResponseMapper.buildResponse(Responses.SUCCESS, stocks);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getInventorySummary(Long warehouseId) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");

            List<Stock> stocks = stockRepository.findByWarehouseId(warehouseId);

            List<StockSummaryDTO> summaries = stocks.stream()
                .filter(stock -> stock.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());

            return ResponseMapper.buildResponse(Responses.SUCCESS, summaries);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getTotalQuantityByItem(Long itemId) {
        try {
            ValidationService.validate(itemId, "Item ID");

            BigDecimal totalQuantity = stockRepository.getTotalQuantityByItem(itemId);

            return ResponseMapper.buildResponse(Responses.SUCCESS, Map.of(
                "itemId", itemId,
                "totalQuantity", totalQuantity
            ));

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getLowStockItems(BigDecimal threshold) {
        try {
            if (threshold == null) {
                threshold = BigDecimal.valueOf(10); // Default threshold
            }

            List<Stock> lowStockItems = stockRepository.findLowStockItems(threshold);

            return ResponseMapper.buildResponse(Responses.SUCCESS, lowStockItems);

        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> adjustStock(StockAdjustmentRequestDTO request, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "Logged in user");
            validateStockAdjustmentRequest(request);

            inventoryService.adjustStock(
                request.getWarehouseId(),
                request.getItemId(),
                request.getQuantity(),
                request.getIncrease(),
                request.getRemarks(),
                loggedInUser
            );

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Stock adjusted successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> transferStock(StockTransferRequestDTO request, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "Logged in user");
            validateStockTransferRequest(request);

            inventoryService.transferStock(
                request.getFromWarehouseId(),
                request.getToWarehouseId(),
                request.getItemId(),
                request.getQuantity(),
                System.currentTimeMillis(), // Use timestamp as refId for transfers
                request.getRemarks(),
                loggedInUser
            );

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Stock transferred successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> reserveStock(Long warehouseId, Long itemId, BigDecimal quantity, String loggedInUser) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");
            ValidationService.validate(itemId, "Item ID");
            ValidationService.validate(quantity, "Quantity");
            ValidationService.validate(loggedInUser, "Logged in user");

            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }

            inventoryService.reserveStock(warehouseId, itemId, quantity, loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Stock reserved successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> releaseReservedStock(Long warehouseId, Long itemId, BigDecimal quantity, String loggedInUser) {
        try {
            ValidationService.validate(warehouseId, "Warehouse ID");
            ValidationService.validate(itemId, "Item ID");
            ValidationService.validate(quantity, "Quantity");
            ValidationService.validate(loggedInUser, "Logged in user");

            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }

            inventoryService.releaseReservedStock(warehouseId, itemId, quantity, loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Reserved stock released successfully");

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ====================== PRIVATE HELPER METHODS ======================

    private StockSummaryDTO convertToSummaryDTO(Stock stock) {
        StockSummaryDTO summary = new StockSummaryDTO();
        summary.setWarehouseId(stock.getWarehouseId());
        summary.setItemId(stock.getItemId());
        summary.setQuantity(stock.getQuantity());
        summary.setReservedQuantity(stock.getReservedQuantity());
        summary.setAvailableQuantity(stock.getQuantity().subtract(stock.getReservedQuantity()));
        summary.setAvgRate(stock.getAvgRate());
        summary.setTotalValue(stock.getQuantity().multiply(stock.getAvgRate()));
        return summary;
    }

    private void validateStockAdjustmentRequest(StockAdjustmentRequestDTO request) {
        ValidationService.validate(request.getWarehouseId(), "Warehouse ID");
        ValidationService.validate(request.getItemId(), "Item ID");
        ValidationService.validate(request.getQuantity(), "Quantity");
        ValidationService.validate(request.getIncrease(), "Increase flag");

        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    private void validateStockTransferRequest(StockTransferRequestDTO request) {
        ValidationService.validate(request.getFromWarehouseId(), "From Warehouse ID");
        ValidationService.validate(request.getToWarehouseId(), "To Warehouse ID");
        ValidationService.validate(request.getItemId(), "Item ID");
        ValidationService.validate(request.getQuantity(), "Quantity");

        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new IllegalArgumentException("Source and destination warehouses cannot be the same");
        }
    }
}
