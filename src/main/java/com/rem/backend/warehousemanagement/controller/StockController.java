package com.rem.backend.warehousemanagement.controller;

import com.rem.backend.warehousemanagement.dto.StockAdjustmentRequestDTO;
import com.rem.backend.warehousemanagement.dto.StockTransferRequestDTO;
import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.warehousemanagement.service.StockService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping("/getByWarehouse")
    public ResponseEntity<?> getStockByWarehouse(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> stockPage = stockService.getStockByWarehouse(request.getId(), pageable);
        return ResponseEntity.ok(stockPage);
    }

    @PostMapping("/getByItem")
    public ResponseEntity<?> getStockByItem(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String, Object> stockPage = stockService.getStockByItem(request.getId(), pageable);
        return ResponseEntity.ok(stockPage);
    }

    @GetMapping("/warehouse/{warehouseId}/item/{itemId}")
    public Map<String, Object> getStockByWarehouseAndItem(@PathVariable Long warehouseId,
                                                          @PathVariable Long itemId) {
        return stockService.getStockByWarehouseAndItem(warehouseId, itemId);
    }

    @GetMapping("/available/warehouse/{warehouseId}")
    public Map<String, Object> getAvailableStockByWarehouse(@PathVariable Long warehouseId) {
        return stockService.getAvailableStockByWarehouse(warehouseId);
    }

    @GetMapping("/available/item/{itemId}")
    public Map<String, Object> getAvailableStockByItem(@PathVariable Long itemId) {
        return stockService.getAvailableStockByItem(itemId);
    }

    @GetMapping("/inventory/summary/{warehouseId}")
    public Map<String, Object> getInventorySummary(@PathVariable Long warehouseId) {
        return stockService.getInventorySummary(warehouseId);
    }

    @GetMapping("/total/item/{itemId}")
    public Map<String, Object> getTotalQuantityByItem(@PathVariable Long itemId) {
        return stockService.getTotalQuantityByItem(itemId);
    }

    @GetMapping("/low-stock")
    public Map<String, Object> getLowStockItems(@RequestParam(required = false) BigDecimal threshold) {
        return stockService.getLowStockItems(threshold);
    }

    @PostMapping("/adjust")
    public Map<String, Object> adjustStock(@Valid @RequestBody StockAdjustmentRequestDTO request,
                                          HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);
        return stockService.adjustStock(request, loggedInUser);
    }

    @PostMapping("/transfer")
    public Map<String, Object> transferStock(@Valid @RequestBody StockTransferRequestDTO request,
                                           HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);
        return stockService.transferStock(request, loggedInUser);
    }

    @PostMapping("/reserve")
    public Map<String, Object> reserveStock(@RequestParam Long warehouseId,
                                           @RequestParam Long itemId,
                                           @RequestParam BigDecimal quantity,
                                           HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);
        return stockService.reserveStock(warehouseId, itemId, quantity, loggedInUser);
    }

    @PostMapping("/release-reservation")
    public Map<String, Object> releaseReservedStock(@RequestParam Long warehouseId,
                                                    @RequestParam Long itemId,
                                                    @RequestParam BigDecimal quantity,
                                                    HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);
        return stockService.releaseReservedStock(warehouseId, itemId, quantity, loggedInUser);
    }
}
