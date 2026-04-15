package com.esprit.microservice.prescriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DosageAnalyticsDTO {
    private String dosage;
    private Integer count;
    private Double percentage;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String recommendation;
}
