package com.esprit.microservice.prescriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FrequencyAnalyticsDTO {
    private String frequency; // jour - daily, weekly, etc.
    private Integer count;
    private Double percentage;
    private Integer totalDosesPerMonth;
    private String complianceRisk; // LOW, MEDIUM, HIGH
}
