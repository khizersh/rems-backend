package com.rem.backend.dto.accounting;

import java.time.LocalDateTime;

public record CreateAccountCategoryResponse(
        long id,
        String name,
        long accountTypeId,
        long organizationId,
        LocalDateTime createdDate
) {}

