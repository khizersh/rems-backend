package com.rem.backend.dto.analytic;

import lombok.Data;

@Data
public class CountStateByTenureRequest {

    private String requestBy;
    private int tenure;
    private long orgId;
}
