package com.esprit.microservice.prescriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionAnalyticsDTO {
    private Long totalPrescriptions;
    private Long totalPatients;
    private Long totalProviders;
    private Double averageDosageComplexity;
    private Double averageFrequencyComplexity;
    
    // Dosage Analysis
    private List<DosageAnalyticsDTO> dosageAnalysis;
    private Integer highRiskDosageCount;
    private String topDosage;
    
    // Frequency Analysis
    private List<FrequencyAnalyticsDTO> frequencyAnalysis;
    private Integer highComplianceRiskCount;
    private String mostCommonFrequency;
    
    // Risk Summary
    private Integer prescriptionsRequiringReview;
    private List<String> recommendations;
    
    // Time-based metrics
    private Double averagePrescriptionsPerPatient;
    private Integer prescriptionsWithComplexity;
}
