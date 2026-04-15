package com.esprit.microservice.prescriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAnalysisReportDTO {
    private Integer totalAnalyzed;
    private Integer criticalCount;
    private Integer highRiskCount;
    private Integer mediumRiskCount;
    private Integer lowRiskCount;
    
    private Double averageRiskScore;
    private Integer maxRiskScore;
    private Integer minRiskScore;
    
    private List<PrescriptionRiskScoreDTO> prescriptionScores;
    private List<String> systemRecommendations;
    private String overallHealthAssessment;
    
    // Trend analysis
    private String riskTrend; // INCREASING, STABLE, DECREASING
    private Integer riskChangePercent;
    
    // Top concerns
    private List<String> topConcerns;
}
