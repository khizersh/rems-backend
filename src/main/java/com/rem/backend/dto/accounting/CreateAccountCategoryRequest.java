package com.rem.backend.dto.accounting;

import lombok.Data;

@Data
public class CreateAccountCategoryRequest {
    private String name;
    private Long accountTypeId;
}

