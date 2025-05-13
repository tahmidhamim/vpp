package com.rore_int.vpp.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatterySearchResponse {
    private List<String> batteryNames;
    private long totalCapacity;
    private double averageCapacity;
}