package com.esprit.microservice.prescriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRiskScoreDTO {
    private Long prescriptionId;
    private Integer overallRiskScore; // 0-100
    private String riskLevel; // LOW, MEDIUM, HIGH
    
    // Component scores
    private Integer dosageRiskScore;
    private Integer frequencyRiskScore;
    private Integer complexityRiskScore;
    private Integer patternRiskScore;
    
    // Details
    private String dosage;
    private String frequency;
    private String patientName;
    private String providerName;
    
    // Recommendations
    private String primaryRecommendation;
    private String secondaryRecommendation;
    private String urgencyLevel; // LOW, MEDIUM, HIGH, CRITICAL
    
    // Comparison
    private Integer percentileRank; // 0-100, where 100 is highest risk
    private Boolean requiresImmediateReview;
}
