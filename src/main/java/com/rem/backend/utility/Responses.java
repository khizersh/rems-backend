package com.rem.backend.utility;

import lombok.AllArgsConstructor;
import lombok.Getter;

public enum Responses {


    SUCCESS("0000", "Request Success!"),
    NO_DATA_FOUND("0001", "No Data Found!"),
    INVALID_PARAMETER("0002", "Invalid Parameter!"),
    SYSTEM_FAILURE("9999", "System Failure!");


    private String responseCode;
    private String responseMessage;

    private Responses(String responseCode, String responseMessage) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }
}
