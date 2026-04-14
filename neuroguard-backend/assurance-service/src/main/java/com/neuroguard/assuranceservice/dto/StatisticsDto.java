package com.neuroguard.assuranceservice.dto;

import lombok.*;
import java.util.*;

/**
 * DTO for comprehensive statistics about assurances and risk assessments
 */
public class StatisticsDto {

    /**
     * Patient-level statistics
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PatientStatistics {
        private Long patientId;
        private String patientName;
        private Integer totalAssurances;
        
        // Risk Metrics
        private Double averageAlzheimersRisk;    // Risque moyen Alzheimer (0-1)
        private Double highestAlzheimersRisk;    // Risque max détecté
        private Double lowestAlzheimersRisk;     // Risque min détecté
        private Double standardDeviationRisk;    // Écart-type du risque
        
        // Cost Metrics
        private Double totalEstimatedCost;       // Coût total annuel
        private Double averageAnnualCost;        // Coût moyen par assurance
        private Double medianAnnualCost;         // Médiane des coûts
        
        // Alert Metrics
        private Integer totalActiveAlerts;       // Total alertes actives
        private Double averageAlertsPerAssurance; // Moyenne alertes/assurance
        private List<String> highestSeverityAlerts; // Top alertes critiques
        
        // Complexity Metrics
        private Double averageComplexityScore;   // Score complexité moyen
        private Integer maxComplexityScore;      // Score max détecté
        
        // Procedure Analysis
        private Map<String, Integer> recommendedProceduresFrequency; // Fréquence des procédures recommandées
        
        // Health Profile
        private Integer careTeamAverageSize;     // Taille moyenne équipe
        private Integer patientsNeedingNeurology; // Nb assurances avec ref neuro
        private Integer patientsNeedingGeriatrics; // Nb assurances avec ref gériatre
        
        // Trend Analysis
        private List<RiskTrendPoint> riskTrend;  // Évolution du risque dans le temps
        private List<CostTrendPoint> costTrend;  // Évolution des coûts
        private String overallRiskLevel;         // VERY_LOW|LOW|MODERATE|HIGH|VERY_HIGH
    }

    /**
     * Assurance-level statistics
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AssuranceStatistics {
        private Long assuranceId;
        private String assuranceType;
        
        // Patient Demographics
        private Integer totalPatientsEnrolled;   // Nombre patients assurés
        private Double averagePatientAge;       // Âge moyen
        
        // Risk Metrics
        private Double averageRiskScore;         // Score risque moyen tous patients
        private Double riskDistribution;         // % patients HIGH RISK
        private Integer patientsHighRisk;        // Nombre patients score > 75
        private Integer patientsMediumRisk;      // Nombre patients score 50-75
        private Integer patientsLowRisk;         // Nombre patients score < 50
        
        // Cost Metrics
        private Double totalProjectedCost;       // Coût total projeté pour assurance
        private Double averageClaimCost;         // Coût moyen par patient
        private Double costVariance;             // Variance des coûts (dispersion)
        private Double costStandardDeviation;    // Écart-type des coûts
        
        // Alzheimer-specific
        private Double averageAlzheimersPrevalence; // % patients avec risque > 50%
        private Integer patientsWithHighAlzRisk;    // Nombre patients risque > 60%
        private Double percentileAlzheimersRisk;    // Percentile du risque
        
        // Procedure Analytics
        private List<ProcedureStatistic> topRecommendedProcedures; // Top 5 procedures
        private Integer totalUniqueProcedures;   // Nombre procédures différentes
        
        // Care Coordination
        private Integer averageCareTeamSize;     // Taille équipe moyenne
        private Integer neurology_referralsNeeded; // Nb referrals neuro
        private Integer geriatricsReferralsNeeded; // Nb referrals gériatrics
        
        // Comparative Analysis
        private Double comparisonToNational;     // Comparaison à moyenne nationale (%)
        private String performanceRating;        // EXCELLENT|GOOD|AVERAGE|POOR
        
        // Temporal Analysis  
        private List<CostTrendPoint> costEvolution; // Coûts sur temps
        private LocalizedDate lastUpdated;
        
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class LocalizedDate {
            private Integer year;
            private Integer month;
            private Integer dayOfMonth;
            private String formattedDate;
        }
    }

    /**
     * System-wide statistics
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SystemStatistics {
        private Integer totalPatients;
        private Integer totalAssurances;
        private Integer totalActiveAlerts;
        
        // Risk Summary
        private Double systemAverageRisk;
        private Integer patientsAtHighRisk;
        private Double percentageHighRiskPopulation;
        
        // Cost Summary
        private Double totalProjectedCost;      // Coûts totaux projetés
        private Double averageCostPerPatient;
        
        // Coverage Distribution
        private Map<String, Integer> coverageDistribution; // BASIC|ENHANCED|COMPREHENSIVE|INTENSIVE count
        
        // Trends
        private List<CostTrendPoint> systemCostTrend;
        private String systemHealthStatus;      // Status global du système
    }

    /**
     * Trend data point
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RiskTrendPoint {
        private String date;                    // Format: YYYY-MM-DD
        private Double riskScore;
        private Integer assessmentCount;
    }

    /**
     * Cost trend data point
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CostTrendPoint {
        private String date;
        private Double totalCost;
        private Double averageCost;
        private Integer assessmentCount;
    }

    /**
     * Procedure statistics
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcedureStatistic {
        private String procedureName;
        private Integer frequency;              // Combien de fois recommandée
        private Double percentageOfAssurances;  // % d'assurances concernées
        private Double averageCostPerProcedure; // Coût moyen de la procédure
    }
}
