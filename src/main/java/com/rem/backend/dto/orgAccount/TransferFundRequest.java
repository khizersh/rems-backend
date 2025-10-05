package com.rem.backend.dto.orgAccount;

import lombok.Data;

@Data
public class TransferFundRequest {

    private Double amount;
    private Long fromAccountId;
    private Long toAccountId;
}
