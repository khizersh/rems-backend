package com.rem.backend.usermanagement.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserWithPermissionsDto {

    private List<String> endPointList;
    private long userId;
    private String username;

}
