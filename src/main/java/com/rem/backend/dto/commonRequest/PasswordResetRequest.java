package com.rem.backend.dto.commonRequest;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PasswordResetRequest {

    private String code;
    private String email;
    private String newPassword;

}
