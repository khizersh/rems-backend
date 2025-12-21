package com.rem.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FeeType {
    FIXED,PERCENTILE;

    @JsonCreator
    public static FeeType from(String value) {
        return FeeType.valueOf(value.toUpperCase());
    }
}
