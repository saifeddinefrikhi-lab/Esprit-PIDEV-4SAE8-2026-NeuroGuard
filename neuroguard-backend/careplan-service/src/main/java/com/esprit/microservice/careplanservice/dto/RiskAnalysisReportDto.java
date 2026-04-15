package com.esprit.microservice.careplanservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAnalysisReportDto {
    private long highRiskCount;
    private long mediumRiskCount;
    private long lowRiskCount;
    private double averageRiskScore;
    private int criticalCount;
    private String riskTrend; // INCREASING, STABLE, DECREASING
    private String overallHealthAssessment;
    private List<String> systemRecommendations;
    private List<PrescriptionRiskScoreDto> prescriptionRisks;
}
