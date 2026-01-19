package com.rem.backend.dto.accounting;

import java.time.LocalDateTime;

public record CreateAccountGroupResponse(
        long id,
        String name,
        long accountTypeId,
        long organizationId,
        LocalDateTime createdDate
) {}

