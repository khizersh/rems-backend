package com.rem.backend.dto.orgAccount;

import lombok.Data;

@Data
public class TransferFundRequest {

    private Double amount;
    private Long fromAccountId;
    private Long toAccountId;

    /**
     * Unique key per intended transfer (e.g. UUID). Retries with the same key return success without
     * applying the transfer again. Optional for backward compatibility; strongly recommended for clients.
     */
    private String idempotencyKey;
}
