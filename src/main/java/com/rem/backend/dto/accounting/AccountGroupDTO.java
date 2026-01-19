package com.rem.backend.dto.accounting;

public record AccountGroupDTO(
        Long id,
        String name,
        AccountTypeDTO accountType,
        String createdDate
) {}

