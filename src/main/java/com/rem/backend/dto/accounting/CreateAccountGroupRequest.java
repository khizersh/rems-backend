package com.rem.backend.dto.accounting;

import lombok.Data;

@Data
public class CreateAccountGroupRequest {
    private String name;
    private Long accountTypeId;  // Should correspond to EXPENSE account type
}

