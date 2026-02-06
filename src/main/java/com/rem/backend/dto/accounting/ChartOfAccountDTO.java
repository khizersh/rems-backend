package com.rem.backend.dto.accounting;

public record ChartOfAccountDTO(
        Long id,
        String code,
        String name,
        AccountGroupDTO accountGroup,
        boolean isSystemGenerated,
        String status,
        Long organizationAccountId,
        String createdDate,
        String updatedDate
) {}

