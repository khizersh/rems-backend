package com.rem.backend.warehousemanagement.controller;

import com.rem.backend.warehousemanagement.dto.ExpenseItemRequestDTO;
import com.rem.backend.warehousemanagement.dto.MaterialIssueRequestDTO;
import com.rem.backend.warehousemanagement.service.WarehouseIntegrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/warehouse-integration")
@RequiredArgsConstructor
public class WarehouseIntegrationController {

    private final WarehouseIntegrationService warehouseIntegrationService;

    @PostMapping("/expense-items")
    public Map<String, Object> processExpenseItems(@Valid @RequestBody ExpenseItemRequestDTO request,
                                                   HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);
        return warehouseIntegrationService.processExpenseItems(request, loggedInUser);
    }

    @PostMapping("/issue-material")
    public Map<String, Object> issueMaterial(@Valid @RequestBody MaterialIssueRequestDTO request,
                                            HttpServletRequest httpRequest) {
        String loggedInUser = (String) httpRequest.getAttribute(LOGGED_IN_USER);
        return warehouseIntegrationService.issueMaterial(
            request.getWarehouseId(),
            request.getItemId(),
            request.getQuantity(),
            request.getProjectId(),
            request.getRemarks(),
            loggedInUser
        );
    }

    @GetMapping("/expense-items/{expenseId}")
    public Map<String, Object> getExpenseItems(@PathVariable Long expenseId) {
        return warehouseIntegrationService.getExpenseItems(expenseId);
    }

    @GetMapping("/stock-effect-items/warehouse/{warehouseId}")
    public Map<String, Object> getStockEffectItemsByWarehouse(@PathVariable Long warehouseId) {
        return warehouseIntegrationService.getStockEffectItemsByWarehouse(warehouseId);
    }
}
