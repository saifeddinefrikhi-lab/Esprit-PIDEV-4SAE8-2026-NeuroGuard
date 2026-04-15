package com.esprit.microservice.careplanservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRiskScoreDto {
    private Long prescriptionId;
    private Long patientId;
    private String patientName;
    private double riskScore; // 0-100
    private String riskLevel; // HIGH, MEDIUM, LOW
    private String recommendation;
    private String medicationName;
    private String interactionDetails;
}
