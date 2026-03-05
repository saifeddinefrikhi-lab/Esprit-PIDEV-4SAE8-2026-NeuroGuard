package com.neuroguard.riskalertservice.dto;

import lombok.Data;

@Data
public class PredictionResponse {
    private Long patientId;
    private int prediction;        // 0 or 1
    private double probability;    // 0.0 to 1.0
    private String riskLevel;      // LOW, MODERATE, HIGH, CRITICAL
    private double riskPercentage; // 0 to 100
    private String recommendation; // Actionable medical guidance
}