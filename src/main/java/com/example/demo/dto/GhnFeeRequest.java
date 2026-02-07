package com.example.demo.dto;

import lombok.Data;

@Data
public class GhnFeeRequest {
    private Integer from_district_id;
    private String from_ward_code;
    private Integer to_district_id;
    private String to_ward_code;
    private Integer service_id;

    private Integer height;
    private Integer length;
    private Integer width;
    private Integer weight;
    private Integer insurance_value;
}

