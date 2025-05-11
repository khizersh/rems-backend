package com.rem.backend.dto.unit;

import lombok.Data;

@Data
public class UnitDetails {

    private long unitId;
    private String unitSerial;
    private long floorId;
    private int floorNo;
    private long projectId;
    private String projectName;
    private double totalAmount;
}
