package com.rem.backend.warehousemanagement.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class ExpenseItemRequestDTO {

    @NotNull(message = "Expense ID is required")
    private Long expenseId;

    @NotNull(message = "Expense items list is required")
    private List<ExpenseItemDTO> expenseItems;

    @Data
    public static class ExpenseItemDTO {

        @NotNull(message = "Item ID is required")
        private Long itemId;

        @NotNull(message = "Unit ID is required")
        private Long unitId;

        @NotNull(message = "Quantity is required")
        private java.math.BigDecimal quantity;

        @NotNull(message = "Rate is required")
        private java.math.BigDecimal rate;

        @NotNull(message = "Amount is required")
        private java.math.BigDecimal amount;

        private Long warehouseId;

        @NotNull(message = "Stock effect flag is required")
        private Boolean stockEffect;
    }
}
