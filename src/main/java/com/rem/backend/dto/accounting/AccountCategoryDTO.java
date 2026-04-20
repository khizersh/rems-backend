package com.rem.backend.dto.accounting;

public record AccountCategoryDTO(
        Long id,
        String name,
        AccountTypeDTO accountType,
        String createdDate
) {}

