package com.neuroguard.assuranceservice.service;

import com.neuroguard.assuranceservice.client.MedicalHistoryClient;
import com.neuroguard.assuranceservice.client.MLPredictorClient;
import com.neuroguard.assuranceservice.client.RiskAlertClient;
import com.neuroguard.assuranceservice.dto.AlertDto;
import com.neuroguard.assuranceservice.dto.MedicalHistoryDto;
import com.neuroguard.assuranceservice.dto.MLPredictionDto;
import com.neuroguard.assuranceservice.entity.CoverageRiskAssessment;
import com.neuroguard.assuranceservice.repository.CoverageRiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoverageRiskAssessmentService {

    private final CoverageRiskAssessmentRepository coverageRiskAssessmentRepository;
    private final MedicalHistoryClient medicalHistoryClient;
    private final MLPredictorClient mlPredictorClient;
    private final RiskAlertClient riskAlertClient;

    private static final Double BASE_CLAIM_COST = 12000.0;

    /**
     * Calculate comprehensive risk assessment for an assurance record
     */
    @Transactional
    public CoverageRiskAssessment calculateRiskAssessment(Long assuranceId, Long patientId) {
        log.info("Starting risk assessment calculation for assurance: {}, patient: {}", assuranceId, patientId);

        try {
            // Step 1: Fetch medical history from authoritative source
            MedicalHistoryDto medicalHistory = fetchMedicalHistory(patientId);
            log.info("Fetched medical history for patient: {} - MMSE: {}, ADL: {}, BMI: {}, Smoking: {}", 
                     patientId, medicalHistory.getMmse(), medicalHistory.getAdl(), medicalHistory.getBmi(), medicalHistory.getSmoking());

            // Step 2: Get ML prediction
            MLPredictionDto mlPrediction = getMlPrediction(medicalHistory, assuranceId);
            log.info("ML prediction score: {} for assuranceId: {}", mlPrediction.getProbability(), assuranceId);

            // Step 3: Fetch active alerts
            List<AlertDto> activeAlerts = fetchActiveAlerts(patientId);
            log.info("Found {} active alerts for patient: {}", activeAlerts.size(), patientId);

            // Step 4: Calculate composite medical complexity score
            Integer medicalComplexityScore = calculateMedicalComplexityScore(
                    mlPrediction, activeAlerts, medicalHistory
            );
            log.info("Calculated medical complexity score: {} for assuranceId: {}", medicalComplexityScore, assuranceId);

            // Step 5: Determine coverage level
            String coverageLevel = determineCoverageLevel(medicalComplexityScore, mlPrediction.getRiskLevel());
            log.info("Determined coverage level: {} for assuranceId: {}", coverageLevel, assuranceId);

            // Step 6: Estimate annual claim cost
            Double estimatedClaimCost = estimateAnnualClaimCost(medicalComplexityScore);
            log.info("Estimated annual claim cost: ${} for assuranceId: {}", estimatedClaimCost, assuranceId);

            // Step 7: Generate recommended procedures
            List<String> recommendedProcedures = generateRecommendedProcedures(medicalHistory, mlPrediction);
            log.info("Generated {} recommended procedures for assuranceId: {}", recommendedProcedures.size(), assuranceId);

            // Step 8: Determine optimal care team
            Integer recommendedProviderCount = deriveRecommendedProviderCount(medicalComplexityScore);
            Boolean neurologyReferralNeeded = mlPrediction.getProbability() > 0.55;
            Boolean geriatricAssessmentNeeded = determineGeriatricAssessmentNeed(medicalHistory, medicalComplexityScore);

            // Step 9: Calculate next assessment date
            LocalDateTime nextAssessmentDate = scheduleNextAssessment(mlPrediction.getRiskLevel());

            // Step 10: Determine risk stratum
            String riskStratum = determineRiskStratum(medicalComplexityScore);

            // Step 11: Analyze alerts
            Map<String, Object> alertAnalysis = analyzeAlerts(activeAlerts);

            // Create and save assessment
            CoverageRiskAssessment assessment = new CoverageRiskAssessment();
            assessment.setAssuranceId(assuranceId);
            assessment.setPatientId(patientId);
            assessment.setAlzheimersPredictionScore(mlPrediction.getProbability());
            assessment.setAlzheimersPredictionLevel(mlPrediction.getRiskLevel());
            assessment.setActiveAlertCount(activeAlerts.size());
            assessment.setHighestAlertSeverity((String) alertAnalysis.get("highestSeverity"));
            assessment.setAlertSeverityRatio((Double) alertAnalysis.get("severityRatio"));
            assessment.setMedicalComplexityScore(medicalComplexityScore);
            assessment.setRecommendedCoverageLevel(coverageLevel);
            assessment.setEstimatedAnnualClaimCost(estimatedClaimCost);
            assessment.setRecommendedProcedures(recommendedProcedures);
            assessment.setRecommendedProviderCount(recommendedProviderCount);
            assessment.setNeurologyReferralNeeded(neurologyReferralNeeded);
            assessment.setGeriatricAssessmentNeeded(geriatricAssessmentNeeded);
            assessment.setLastAssessmentDate(LocalDateTime.now());
            assessment.setNextRecommendedAssessmentDate(nextAssessmentDate);
            assessment.setRiskStratum(riskStratum);

            CoverageRiskAssessment saved = coverageRiskAssessmentRepository.save(assessment);
            log.info("Successfully saved risk assessment with ID: {} for assuranceId: {} - ComplexityScore: {}, Cost: ${}", 
                     saved.getId(), assuranceId, medicalComplexityScore, estimatedClaimCost);

            return saved;

        } catch (Exception e) {
            log.error("Error calculating risk assessment for assurance: {}, patient: {}", assuranceId, patientId, e);
            throw new RuntimeException("Failed to calculate coverage risk assessment: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch medical history from medical-history-service
     */
    private MedicalHistoryDto fetchMedicalHistory(Long patientId) {
        try {
            return medicalHistoryClient.getMedicalHistoryByPatientId(patientId);
        } catch (Exception e) {
            log.warn("Failed to fetch medical history for patient: {}", patientId, e);
            return new MedicalHistoryDto();
        }
    }

    /**
     * Get ML prediction from ml-predictor-service
     */
    private MLPredictionDto getMlPrediction(MedicalHistoryDto medicalHistory, Long assuranceId) {
        try {
            Map<String, Object> features = buildFeatureMap(medicalHistory, assuranceId);
            return mlPredictorClient.predict(features);
        } catch (Exception e) {
            log.warn("Failed to get ML prediction", e);
            // Return neutral prediction on failure
            return new MLPredictionDto("0", 0.25, "MODERATE", "Default prediction", "Consult provider");
        }
    }

    /**
     * Build feature map for ML prediction using actual patient medical data
     */
    private Map<String, Object> buildFeatureMap(MedicalHistoryDto medicalHistory, Long assuranceId) {
        Map<String, Object> features = new HashMap<>();
        
        // Use actual patient data without seed-based variation
        features.put("mmse", medicalHistory.getMmse() != null ? medicalHistory.getMmse() : 24);
        features.put("adl", medicalHistory.getAdl() != null ? medicalHistory.getAdl() : 6);
        features.put("functional_assessment", medicalHistory.getFunctionalAssessment() != null ? medicalHistory.getFunctionalAssessment() : 4);
        features.put("smoking", medicalHistory.getSmoking() != null ? medicalHistory.getSmoking() : false);
        features.put("cardiovascular_disease", medicalHistory.getCardiovascularDisease() != null ? medicalHistory.getCardiovascularDisease() : false);
        features.put("diabetes", medicalHistory.getDiabetes() != null ? medicalHistory.getDiabetes() : false);
        features.put("depression", medicalHistory.getDepression() != null ? medicalHistory.getDepression() : false);
        features.put("head_injury", medicalHistory.getHeadInjury() != null ? medicalHistory.getHeadInjury() : false);
        features.put("hypertension", medicalHistory.getHypertension() != null ? medicalHistory.getHypertension() : false);
        features.put("bmi", medicalHistory.getBmi() != null ? medicalHistory.getBmi() : 25.0);
        features.put("cholesterol_total", medicalHistory.getCholesterolTotal() != null ? medicalHistory.getCholesterolTotal() : 200.0);
        features.put("alcohol_consumption", medicalHistory.getAlcoholConsumption() != null ? medicalHistory.getAlcoholConsumption() : 1);
        features.put("physical_activity", medicalHistory.getPhysicalActivity() != null ? medicalHistory.getPhysicalActivity() : 4);
        features.put("diet_quality", medicalHistory.getDietQuality() != null ? medicalHistory.getDietQuality() : 4);
        features.put("sleep_quality", medicalHistory.getSleepQuality() != null ? medicalHistory.getSleepQuality() : 3);
        features.put("memory_complaints", medicalHistory.getMemoryComplaints() != null ? medicalHistory.getMemoryComplaints() : false);
        features.put("behavioral_problems", medicalHistory.getBehavioralProblems() != null ? medicalHistory.getBehavioralProblems() : false);

        log.debug("Generated features for patient from medical history - MMSE: {}, ADL: {}, Smoking: {}", 
                  features.get("mmse"), features.get("adl"), features.get("smoking"));
        return features;
    }

    /**
     * Fetch active alerts from risk-alert-service
     */
    private List<AlertDto> fetchActiveAlerts(Long patientId) {
        try {
            return riskAlertClient.getPatientAlerts(patientId);
        } catch (Exception e) {
            log.warn("Failed to fetch alerts for patient: {}", patientId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Calculate composite medical complexity score (0-100) based on actual patient data
     */
    private Integer calculateMedicalComplexityScore(
            MLPredictionDto mlPrediction,
            List<AlertDto> alerts,
            MedicalHistoryDto medicalHistory) {

        double score = 0.0;

        // Component 1: ML Prediction Score (35% weight)
        double mlComponent = mlPrediction.getProbability() * 35.0;
        score += Math.max(0, Math.min(35, mlComponent));
        log.debug("ML component: {} (base: {})", Math.max(0, Math.min(35, mlComponent)), mlPrediction.getProbability() * 35.0);

        // Component 2: Alert Pattern Analysis (23% weight)
        long criticalCount = alerts.stream().filter(a -> "CRITICAL".equals(a.getSeverity())).count();
        long warningCount = alerts.stream().filter(a -> "WARNING".equals(a.getSeverity())).count();
        double alertComponent = (criticalCount * 15.0) + (warningCount * 8.0);
        score += alertComponent;
        log.debug("Alert component - critical: {}, warning: {}, total: {}", criticalCount, warningCount, alertComponent);

        // Component 3: Cognitive Metrics (15% weight)
        int cognitiveScore = 0;
        if (medicalHistory.getMmse() != null && medicalHistory.getMmse() < 24) {
            cognitiveScore += 10;
        }
        if (medicalHistory.getFunctionalAssessment() != null && medicalHistory.getFunctionalAssessment() < 4) {
            cognitiveScore += 8;
        }
        if (medicalHistory.getAdl() != null && medicalHistory.getAdl() < 6) {
            cognitiveScore += 9;
        }
        score += cognitiveScore;
        log.debug("Cognitive component: {} (max 27)", cognitiveScore);

        // Component 4: Comorbidity Burden (10% weight)
        int comorbidityCount = countComorbidities(medicalHistory);
        score += Math.min(10.0, comorbidityCount * 2.0);
        log.debug("Comorbidity component: {} (count: {})", Math.min(10.0, comorbidityCount * 2.0), comorbidityCount);

        // Component 5: Risk Factor Accumulation (10% weight)
        double riskFactorScore = 0.0;
        if (medicalHistory.getCardiovascularDisease() != null && medicalHistory.getCardiovascularDisease() &&
            medicalHistory.getDiabetes() != null && medicalHistory.getDiabetes()) {
            riskFactorScore += 4.0;
        }
        if (medicalHistory.getSmoking() != null && medicalHistory.getSmoking() &&
            medicalHistory.getHypertension() != null && medicalHistory.getHypertension()) {
            riskFactorScore += 3.0;
        }
        if (medicalHistory.getDepression() != null && medicalHistory.getDepression() &&
            medicalHistory.getHeadInjury() != null && medicalHistory.getHeadInjury()) {
            riskFactorScore += 2.0;
        }
        score += Math.min(10.0, riskFactorScore);
        log.debug("Risk factor component: {}", Math.min(10.0, riskFactorScore));

        // Component 6: Allergy Risk (7% weight)
        double allergyScore = 0.0;
        if (medicalHistory.getMedicationAllergies() != null && !medicalHistory.getMedicationAllergies().isEmpty()) {
            allergyScore += 2.5;
        }
        if (medicalHistory.getEnvironmentalAllergies() != null && !medicalHistory.getEnvironmentalAllergies().isEmpty()) {
            allergyScore += 2.0;
        }
        if (medicalHistory.getFoodAllergies() != null && !medicalHistory.getFoodAllergies().isEmpty()) {
            allergyScore += 2.5;
        }
        score += Math.min(7.0, allergyScore);
        log.debug("Allergy component: {}", Math.min(7.0, allergyScore));

        int finalScore = Math.min(100, (int) score);
        log.debug("Final complexity score: {} (raw: {})", finalScore, score);
        return finalScore;
    }

    /**
     * Count active comorbidities
     */
    private int countComorbidities(MedicalHistoryDto medicalHistory) {
        int count = 0;
        if (medicalHistory.getSmoking() != null && medicalHistory.getSmoking()) count++;
        if (medicalHistory.getCardiovascularDisease() != null && medicalHistory.getCardiovascularDisease()) count++;
        if (medicalHistory.getDiabetes() != null && medicalHistory.getDiabetes()) count++;
        if (medicalHistory.getDepression() != null && medicalHistory.getDepression()) count++;
        if (medicalHistory.getHeadInjury() != null && medicalHistory.getHeadInjury()) count++;
        if (medicalHistory.getHypertension() != null && medicalHistory.getHypertension()) count++;
        return count;
    }

    /**
     * Determine coverage level based on complexity score
     */
    private String determineCoverageLevel(Integer score, String mlRiskLevel) {
        if (score >= 85 || "CRITICAL".equals(mlRiskLevel)) {
            return "INTENSIVE";
        } else if (score >= 65 || "HIGH".equals(mlRiskLevel)) {
            return "COMPREHENSIVE";
        } else if (score >= 45 || "MODERATE".equals(mlRiskLevel)) {
            return "ENHANCED";
        } else {
            return "BASIC";
        }
    }

    /**
     * Estimate annual claim cost based on medical complexity alone
     */
    private Double estimateAnnualClaimCost(Integer complexityScore) {
        // Base formula without arbitrary seed variation
        double complexityComponent = (complexityScore / 100.0 * 3.0); // Up to 3x multiplier
        double totalMultiplier = 1.0 + complexityComponent;
        double estimatedCost = BASE_CLAIM_COST * totalMultiplier;
        
        log.debug("Estimated cost calculation - complexityScore: {}, totalMultiplier: {}, FINAL COST: ${}",
                  complexityScore, totalMultiplier, estimatedCost);
        return estimatedCost;
    }

    /**
     * Generate recommended procedures based on medical profile
     */
    private List<String> generateRecommendedProcedures(MedicalHistoryDto medicalHistory, MLPredictionDto mlPrediction) {
        List<String> procedures = new ArrayList<>();

        if (mlPrediction.getProbability() > 0.60) {
            procedures.add("Neuro-psychological assessment");
        }

        if (medicalHistory.getMmse() != null && medicalHistory.getMmse() < 18) {
            procedures.add("Cognitive rehabilitation program");
        }

        if (medicalHistory.getCardiovascularDisease() != null && medicalHistory.getCardiovascularDisease() &&
            medicalHistory.getDiabetes() != null && medicalHistory.getDiabetes()) {
            procedures.add("Cardiovascular imaging");
        }

        if (medicalHistory.getFunctionalAssessment() != null && medicalHistory.getFunctionalAssessment() < 5) {
            procedures.add("Occupational therapy assessment");
        }

        if (medicalHistory.getFamilyHistory() != null && medicalHistory.getFamilyHistory().toLowerCase().contains("dementia")) {
            procedures.add("Genetic counseling");
        }

        if (medicalHistory.getAdl() != null && medicalHistory.getAdl() < 4) {
            procedures.add("Home care assessment");
        }

        if (mlPrediction.getProbability() > 0.70) {
            procedures.add("Advanced neuroimaging (MRI/PET)");
        }

        return procedures;
    }

    /**
     * Determine recommended provider count based on complexity
     */
    private Integer deriveRecommendedProviderCount(Integer complexityScore) {
        if (complexityScore >= 70) {
            return 4;  // Multi-disciplinary team
        } else if (complexityScore >= 50) {
            return 3;  // Primary + Neurology + Cardiology
        } else if (complexityScore >= 30) {
            return 2;  // Primary + Specialist
        } else {
            return 1;  // Primary care only
        }
    }

    /**
     * Determine if geriatric assessment is needed
     */
    private Boolean determineGeriatricAssessmentNeed(MedicalHistoryDto medicalHistory, Integer complexityScore) {
        // This would need age data from user service in real implementation
        return complexityScore > 40;
    }

    /**
     * Schedule next assessment based on risk level
     */
    private LocalDateTime scheduleNextAssessment(String riskLevel) {
        LocalDateTime now = LocalDateTime.now();
        switch (riskLevel) {
            case "CRITICAL":
            case "HIGH":
                return now.plusMonths(3);  // Review in 3 months
            case "MODERATE":
                return now.plusMonths(6);  // Review in 6 months
            default:
                return now.plusMonths(12); // Review in 12 months
        }
    }

    /**
     * Determine risk stratum
     */
    private String determineRiskStratum(Integer score) {
        if (score >= 80) {
            return "VERY_HIGH";
        } else if (score >= 60) {
            return "HIGH";
        } else if (score >= 40) {
            return "MODERATE";
        } else if (score >= 20) {
            return "LOW";
        } else {
            return "VERY_LOW";
        }
    }

    /**
     * Analyze alert patterns
     */
    private Map<String, Object> analyzeAlerts(List<AlertDto> alerts) {
        Map<String, Object> analysis = new HashMap<>();

        long criticalCount = alerts.stream().filter(a -> "CRITICAL".equals(a.getSeverity())).count();
        long warningCount = alerts.stream().filter(a -> "WARNING".equals(a.getSeverity())).count();
        long totalAlerts = alerts.size();

        String highestSeverity = "INFO";
        if (criticalCount > 0) {
            highestSeverity = "CRITICAL";
        } else if (warningCount > 0) {
            highestSeverity = "WARNING";
        }

        double severityRatio = totalAlerts > 0 ? (double) (criticalCount + warningCount) / totalAlerts : 0.0;

        analysis.put("highestSeverity", highestSeverity);
        analysis.put("severityRatio", severityRatio);

        return analysis;
    }


    /**
     * Get existing assessment or null
     */
    public CoverageRiskAssessment getAssessmentByAssuranceId(Long assuranceId) {
        return coverageRiskAssessmentRepository.findByAssuranceId(assuranceId).orElse(null);
    }

    /**
     * Recalculate and refresh an existing risk assessment - updates existing or creates new
     */
    @Transactional
    public CoverageRiskAssessment recalculateRiskAssessment(Long assuranceId, Long patientId) {
        log.info("Recalculating risk assessment for assuranceId: {}, patientId: {}", assuranceId, patientId);
        
        try {
            // Check if assessment already exists for this assurance
            var existingAssessment = coverageRiskAssessmentRepository.findByAssuranceId(assuranceId);
            
            if (existingAssessment.isPresent()) {
                log.info("Found existing assessment for assuranceId: {}, updating it", assuranceId);
                // Update the existing assessment instead of creating a new one
                return updateExistingAssessment(existingAssessment.get(), assuranceId, patientId);
            } else {
                log.info("No existing assessment found for assuranceId: {}, creating new one", assuranceId);
                // Generate fresh assessment with new seed-based variation
                CoverageRiskAssessment assessment = calculateRiskAssessment(assuranceId, patientId);
                log.info("Successfully created new risk assessment for assuranceId: {}", assuranceId);
                return assessment;
            }
        } catch (Exception e) {
            log.error("Error recalculating risk assessment for assuranceId: {}, patientId: {}", assuranceId, patientId, e);
            // Fallback: try to create without deleting
            return calculateRiskAssessment(assuranceId, patientId);
        }
    }
    
    /**
     * Update an existing risk assessment with new calculated values
     */
    @Transactional
    private CoverageRiskAssessment updateExistingAssessment(CoverageRiskAssessment assessment, Long assuranceId, Long patientId) {
        log.info("Updating existing risk assessment for assuranceId: {}, patientId: {}", assuranceId, patientId);
        
        try {
            // Step 1: Fetch medical history from authoritative source
            MedicalHistoryDto medicalHistory = fetchMedicalHistory(patientId);
            
            // Step 2: Get ML prediction
            MLPredictionDto mlPrediction = getMlPrediction(medicalHistory, assuranceId);
            
            // Step 3: Fetch active alerts
            List<AlertDto> activeAlerts = fetchActiveAlerts(patientId);
            
            // Step 4: Calculate composite medical complexity score
            Integer medicalComplexityScore = calculateMedicalComplexityScore(
                    mlPrediction, activeAlerts, medicalHistory
            );
            
            // Step 5: Determine coverage level
            String coverageLevel = determineCoverageLevel(medicalComplexityScore, mlPrediction.getRiskLevel());
            
            // Step 6: Estimate annual claim cost
            Double estimatedClaimCost = estimateAnnualClaimCost(medicalComplexityScore);
            
            // Step 7: Generate recommended procedures
            List<String> recommendedProcedures = generateRecommendedProcedures(medicalHistory, mlPrediction);
            
            // Step 8: Determine optimal care team
            Integer recommendedProviderCount = deriveRecommendedProviderCount(medicalComplexityScore);
            Boolean neurologyReferralNeeded = mlPrediction.getProbability() > 0.55;
            Boolean geriatricAssessmentNeeded = determineGeriatricAssessmentNeed(medicalHistory, medicalComplexityScore);
            
            // Step 9: Calculate next assessment date
            LocalDateTime nextAssessmentDate = scheduleNextAssessment(mlPrediction.getRiskLevel());
            
            // Step 10: Determine risk stratum
            String riskStratum = determineRiskStratum(medicalComplexityScore);
            
            // Step 11: Analyze alerts
            Map<String, Object> alertAnalysis = analyzeAlerts(activeAlerts);
            
            // Update existing assessment with new values
            assessment.setPatientId(patientId);
            assessment.setAlzheimersPredictionScore(mlPrediction.getProbability());
            assessment.setAlzheimersPredictionLevel(mlPrediction.getRiskLevel());
            assessment.setActiveAlertCount(activeAlerts.size());
            assessment.setHighestAlertSeverity((String) alertAnalysis.get("highestSeverity"));
            assessment.setAlertSeverityRatio((Double) alertAnalysis.get("severityRatio"));
            assessment.setMedicalComplexityScore(medicalComplexityScore);
            assessment.setRecommendedCoverageLevel(coverageLevel);
            assessment.setEstimatedAnnualClaimCost(estimatedClaimCost);
            assessment.setRecommendedProcedures(recommendedProcedures);
            assessment.setRecommendedProviderCount(recommendedProviderCount);
            assessment.setNeurologyReferralNeeded(neurologyReferralNeeded);
            assessment.setGeriatricAssessmentNeeded(geriatricAssessmentNeeded);
            assessment.setLastAssessmentDate(LocalDateTime.now());
            assessment.setNextRecommendedAssessmentDate(nextAssessmentDate);
            assessment.setRiskStratum(riskStratum);
            
            CoverageRiskAssessment updated = coverageRiskAssessmentRepository.save(assessment);
            log.info("Successfully updated risk assessment with ID: {} for assuranceId: {} - ComplexityScore: {}, Cost: ${}", 
                     updated.getId(), assuranceId, medicalComplexityScore, estimatedClaimCost);
            
            return updated;
        } catch (Exception e) {
            log.error("Error updating risk assessment for assuranceId: {}, patientId: {}", assuranceId, patientId, e);
            throw new RuntimeException("Failed to update coverage risk assessment: " + e.getMessage(), e);
        }
    }
}
