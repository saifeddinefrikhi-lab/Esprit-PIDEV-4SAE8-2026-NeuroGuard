package com.neuroguard.assuranceservice.service;

import com.neuroguard.assuranceservice.dto.StatisticsDto;
import com.neuroguard.assuranceservice.dto.StatisticsDto.*;
import com.neuroguard.assuranceservice.entity.Assurance;
import com.neuroguard.assuranceservice.entity.CoverageRiskAssessment;
import com.neuroguard.assuranceservice.repository.AssuranceRepository;
import com.neuroguard.assuranceservice.repository.CoverageRiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final AssuranceRepository assuranceRepository;
    private final CoverageRiskAssessmentRepository riskAssessmentRepository;

    /**
     * Calculate patient-level statistics
     */
    public PatientStatistics getPatientStatistics(Long patientId) {
        log.info("Calculating statistics for patient: {}", patientId);
        
        PatientStatistics stats = new PatientStatistics();
        stats.setPatientId(patientId);
        
        // Get all assurances for patient
        List<Assurance> patientAssurances = assuranceRepository.findByPatientId(patientId);
        stats.setTotalAssurances(patientAssurances.size());
        
        if (patientAssurances.isEmpty()) {
            log.warn("No assurances found for patient: {}", patientId);
            return stats;
        }
        
        // Get all risk assessments for patient
        List<CoverageRiskAssessment> assessments = patientAssurances.stream()
                .map(Assurance::getCoverageRiskAssessment)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (assessments.isEmpty()) {
            return stats;
        }
        
        // ============ RISK METRICS CALCULATIONS ============
        List<Double> riskScores = assessments.stream()
                .map(CoverageRiskAssessment::getAlzheimersPredictionScore)
                .collect(Collectors.toList());
        
        stats.setAverageAlzheimersRisk(calculateMean(riskScores));
        stats.setHighestAlzheimersRisk(riskScores.stream().max(Double::compareTo).orElse(0.0));
        stats.setLowestAlzheimersRisk(riskScores.stream().min(Double::compareTo).orElse(0.0));
        stats.setStandardDeviationRisk(calculateStandardDeviation(riskScores));
        
        // ============ COST METRICS CALCULATIONS ============
        List<Double> costs = assessments.stream()
                .map(CoverageRiskAssessment::getEstimatedAnnualClaimCost)
                .collect(Collectors.toList());
        
        stats.setTotalEstimatedCost(calculateSum(costs));
        stats.setAverageAnnualCost(calculateMean(costs));
        stats.setMedianAnnualCost(calculateMedian(costs));
        
        // ============ ALERT METRICS ============
        Integer totalAlerts = assessments.stream()
                .mapToInt(CoverageRiskAssessment::getActiveAlertCount)
                .sum();
        stats.setTotalActiveAlerts(totalAlerts);
        stats.setAverageAlertsPerAssurance((double) totalAlerts / assessments.size());
        
        List<String> severities = assessments.stream()
                .map(CoverageRiskAssessment::getHighestAlertSeverity)
                .filter(s -> "CRITICAL".equals(s) || "WARNING".equals(s))
                .collect(Collectors.toList());
        stats.setHighestSeverityAlerts(severities);
        
        // ============ COMPLEXITY METRICS ============
        List<Integer> complexityScores = assessments.stream()
                .map(CoverageRiskAssessment::getMedicalComplexityScore)
                .collect(Collectors.toList());
        
        stats.setAverageComplexityScore(calculateMeanInt(complexityScores));
        stats.setMaxComplexityScore(complexityScores.stream().max(Integer::compareTo).orElse(0));
        
        // ============ PROCEDURE FREQUENCY ANALYSIS ============
        Map<String, Integer> procedureFrequency = new HashMap<>();
        assessments.forEach(assessment -> {
            if (assessment.getRecommendedProcedures() != null) {
                assessment.getRecommendedProcedures().forEach(proc ->
                    procedureFrequency.merge(proc, 1, Integer::sum)
                );
            }
        });
        stats.setRecommendedProceduresFrequency(procedureFrequency);
        
        // ============ CARE TEAM ANALYSIS ============
        Integer totalProviders = assessments.stream()
                .mapToInt(CoverageRiskAssessment::getRecommendedProviderCount)
                .sum();
        stats.setCareTeamAverageSize(totalProviders / assessments.size());
        
        long neuroReferrals = assessments.stream()
                .filter(CoverageRiskAssessment::getNeurologyReferralNeeded)
                .count();
        stats.setPatientsNeedingNeurology((int) neuroReferrals);
        
        long geriatricReferrals = assessments.stream()
                .filter(CoverageRiskAssessment::getGeriatricAssessmentNeeded)
                .count();
        stats.setPatientsNeedingGeriatrics((int) geriatricReferrals);
        
        // ============ OVERALL RISK LEVEL ============
        Double avgRisk = stats.getAverageAlzheimersRisk();
        stats.setOverallRiskLevel(determineRiskLevel(avgRisk, stats.getAverageComplexityScore()));
        
        log.info("Calculated statistics for patient: {} - Avg Risk: {}, Total Cost: ${}", 
                 patientId, stats.getAverageAlzheimersRisk(), stats.getTotalEstimatedCost());
        
        return stats;
    }

    /**
     * Calculate assurance-level statistics
     */
    public AssuranceStatistics getAssuranceStatistics(Long assuranceId) {
        log.info("Calculating statistics for assurance: {}", assuranceId);
        
        AssuranceStatistics stats = new AssuranceStatistics();
        stats.setAssuranceId(assuranceId);
        
        Assurance assurance = assuranceRepository.findById(assuranceId).orElse(null);
        if (assurance == null) {
            log.warn("Assurance not found: {}", assuranceId);
            return stats;
        }
        
        // Get all risk assessments for this assurance
        List<CoverageRiskAssessment> assessments = new ArrayList<>();
        if (assurance.getCoverageRiskAssessment() != null) {
            assessments.add(assurance.getCoverageRiskAssessment());
        }
        
        if (assessments.isEmpty()) {
            return stats;
        }
        
        // ============ RISK METRICS ============
        List<Double> riskScores = assessments.stream()
                .map(CoverageRiskAssessment::getAlzheimersPredictionScore)
                .collect(Collectors.toList());
        
        stats.setAverageRiskScore(calculateMean(riskScores));
        
        // Count risk categories
        long highRisk = riskScores.stream().filter(r -> r > 0.75).count();
        long mediumRisk = riskScores.stream().filter(r -> r >= 0.5 && r <= 0.75).count();
        long lowRisk = riskScores.stream().filter(r -> r < 0.5).count();
        
        stats.setPatientsHighRisk((int) highRisk);
        stats.setPatientsMediumRisk((int) mediumRisk);
        stats.setPatientsLowRisk((int) lowRisk);
        stats.setRiskDistribution((double) highRisk / assessments.size() * 100);
        
        // ============ COST METRICS ============
        List<Double> costs = assessments.stream()
                .map(CoverageRiskAssessment::getEstimatedAnnualClaimCost)
                .collect(Collectors.toList());
        
        stats.setTotalProjectedCost(calculateSum(costs));
        stats.setAverageClaimCost(calculateMean(costs));
        stats.setCostVariance(calculateVariance(costs));
        stats.setCostStandardDeviation(Math.sqrt(stats.getCostVariance()));
        
        // ============ ALZHEIMER PREVALENCE ============
        long highAlzRisk = riskScores.stream().filter(r -> r > 0.6).count();
        stats.setPatientsWithHighAlzRisk((int) highAlzRisk);
        stats.setAverageAlzheimersPrevalence((double) highAlzRisk / assessments.size() * 100);
        
        // ============ PROCEDURE ANALYTICS ============
        Map<String, Integer> procedureCount = new HashMap<>();
        assessments.forEach(a -> {
            if (a.getRecommendedProcedures() != null) {
                a.getRecommendedProcedures().forEach(p ->
                    procedureCount.merge(p, 1, Integer::sum)
                );
            }
        });
        
        List<ProcedureStatistic> topProcedures = procedureCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> new ProcedureStatistic(
                    e.getKey(),
                    e.getValue(),
                    (double) e.getValue() / assessments.size() * 100,
                    50.0 // Exemple de coût moyen
                ))
                .collect(Collectors.toList());
        
        stats.setTopRecommendedProcedures(topProcedures);
        stats.setTotalUniqueProcedures(procedureCount.size());
        
        // ============ CARE COORDINATION ============
        Integer totalProviders = assessments.stream()
                .mapToInt(CoverageRiskAssessment::getRecommendedProviderCount)
                .sum();
        stats.setAverageCareTeamSize(totalProviders / assessments.size());
        
        long neuroNeeded = assessments.stream()
                .filter(CoverageRiskAssessment::getNeurologyReferralNeeded)
                .count();
        stats.setNeurology_referralsNeeded((int) neuroNeeded);
        
        long geriatricNeeded = assessments.stream()
                .filter(CoverageRiskAssessment::getGeriatricAssessmentNeeded)
                .count();
        stats.setGeriatricsReferralsNeeded((int) geriatricNeeded);
        
        // ============ PERFORMANCE RATING ============
        Double avgRisk = stats.getAverageRiskScore();
        if (avgRisk < 0.4) {
            stats.setPerformanceRating("EXCELLENT");
        } else if (avgRisk < 0.6) {
            stats.setPerformanceRating("GOOD");
        } else if (avgRisk < 0.75) {
            stats.setPerformanceRating("AVERAGE");
        } else {
            stats.setPerformanceRating("POOR");
        }
        
        stats.setComparisonToNational(((avgRisk / 0.5) - 1) * 100); // Comparé à 0.5 (baseline)
        
        log.info("Calculated statistics for assurance: {} - Avg Risk: {}, Total Cost: ${}", 
                 assuranceId, stats.getAverageRiskScore(), stats.getTotalProjectedCost());
        
        return stats;
    }

    // ============ MATHEMATICAL HELPER METHODS ============

    /**
     * Calculate arithmetic mean
     */
    private Double calculateMean(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private Double calculateMeanInt(List<Integer> values) {
        if (values.isEmpty()) return 0.0;
        return values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    /**
     * Calculate median
     */
    private Double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2;
        } else {
            return sorted.get(size / 2);
        }
    }

    /**
     * Calculate sum
     */
    private Double calculateSum(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Calculate standard deviation
     */
    private Double calculateStandardDeviation(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        Double mean = calculateMean(values);
        Double variance = calculateVariance(values);
        return Math.sqrt(variance);
    }

    /**
     * Calculate variance
     */
    private Double calculateVariance(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        Double mean = calculateMean(values);
        Double sumSquaredDiff = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum();
        return sumSquaredDiff / values.size();
    }

    /**
     * Determine overall risk level based on average Alzheimer risk and complexity
     */
    private String determineRiskLevel(Double alzheimersRisk, Double complexity) {
        Double combinedScore = (alzheimersRisk * 100 + complexity) / 2;
        
        if (combinedScore < 25) return "VERY_LOW";
        if (combinedScore < 45) return "LOW";
        if (combinedScore < 65) return "MODERATE";
        if (combinedScore < 85) return "HIGH";
        return "VERY_HIGH";
    }
}
