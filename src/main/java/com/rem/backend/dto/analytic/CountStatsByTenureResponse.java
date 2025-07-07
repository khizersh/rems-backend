package com.rem.backend.dto.analytic;

import lombok.Data;

@Data
public class CountStatsByTenureResponse {

    private String title;
    private double value;
    private double percentage;
    private String tenure;

}
