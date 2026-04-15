package com.neuroguard.assuranceservice.service;

import com.neuroguard.assuranceservice.client.ConsultationClient;
import com.neuroguard.assuranceservice.dto.ConsultationResponseDto;
import com.neuroguard.assuranceservice.dto.ProcedureSimulationDto;
import com.neuroguard.assuranceservice.dto.SimulationResponseDto;
import com.neuroguard.assuranceservice.entity.Assurance;
import com.neuroguard.assuranceservice.repository.AssuranceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final ConsultationClient consultationClient;
    private final AssuranceRepository assuranceRepository;

    private static final Map<String, Double> PROCEDURE_COSTS = new HashMap<>();

    static {
        PROCEDURE_COSTS.put("CONSULTATION_GENERAL", 60.0);
        PROCEDURE_COSTS.put("CONSULTATION_SPECIALIST", 120.0);
        PROCEDURE_COSTS.put("MRI_SCAN", 450.0);
        PROCEDURE_COSTS.put("BLOOD_TEST", 85.0);
        PROCEDURE_COSTS.put("COGNITIVE_THERAPY", 150.0);
        PROCEDURE_COSTS.put("HOME_CARE_SESSION", 75.0);
    }

    public ProcedureSimulationDto simulateProcedure(Long patientId, String procedureName) {
        log.info("Simulating reimbursement for patient: {}, procedure: {}", patientId, procedureName);
        
        Assurance assurance = assuranceRepository.findByPatientId(patientId)
                .stream().findFirst()
                .orElse(null);

        Double totalCost = PROCEDURE_COSTS.getOrDefault(procedureName, 100.0);
        
        // Default to BASIC 70% if no insurance found
        Double coveragePercent = 0.70; 
        String coverageLevel = "No Plan Found (Simulated Basic)";

        if (assurance != null) {
            coverageLevel = assurance.getCoverageDetails();
            String details = coverageLevel.toUpperCase();
            if (details.contains("INTENSIVE") || details.contains("100%")) coveragePercent = 1.0;
            else if (details.contains("COMPREHENSIVE") || details.contains("85%")) coveragePercent = 0.85;
            else if (details.contains("ENHANCED") || details.contains("75%")) coveragePercent = 0.75;
        }
        
        Double reimbursement = totalCost * coveragePercent;
        
        return ProcedureSimulationDto.builder()
                .procedureName(procedureName)
                .totalBaseCost(totalCost)
                .insuranceCoverage(coveragePercent)
                .insuranceReimbursement(reimbursement)
                .patientRemainder(totalCost - reimbursement)
                .coverageLevel(coverageLevel)
                .build();
    }

    public SimulationResponseDto getRentabilityAnalysis(Long patientId) {
        log.info("Performing rentability analysis for patient: {}", patientId);
        
        Assurance assurance = assuranceRepository.findByPatientId(patientId)
                .stream().findFirst()
                .orElse(null);

        if (assurance == null) {
            log.info("No insurance found for patient: {}", patientId);
            // Return default response for patient without insurance
            return SimulationResponseDto.builder()
                    .patientId(patientId)
                    .currentPlan("NO_PLAN")
                    .monthlyPremium(0.0)
                    .totalPremiumsPaid(0.0)
                    .totalBenefitsReceived(0.0)
                    .benefitCostRatio(0.0)
                    .rentabilityStatus("NO_PLAN")
                    .optimizationAdvice(List.of("No insurance plan activated. Subscribe to a plan to get healthcare coverage and analysis."))
                    .recommendedPlan("BASIC")
                    .potentialAnnualSavings(960.0) // BASIC plan annual cost
                    .consultationCount(0)
                    .estimatedTotalCost(0.0)
                    .build();
        }

        // Fetch consultation history to analyze actual usage
        List<ConsultationResponseDto> history = new ArrayList<>();
        try {
            history = consultationClient.getConsultationsByPatient(patientId);
            log.info("Retrieved {} consultations for patient: {}", history.size(), patientId);
        } catch (Exception e) {
            log.warn("Failed to fetch consultation history for patient {}: {}", patientId, e.getMessage());
        }

        // Determine plan pricing and coverage based on coverage level
        String coverageDetails = assurance.getCoverageDetails();
        Double monthlyPremium = getPremiumForPlan(coverageDetails);
        Integer monthsActive = 12; // Assume annual analysis
        
        // Calculate total benefits received (actual consultation costs saved by insurance)
        Double averageConsultationCost = 100.0; // Average real-world consultation cost
        Double totalSavedByInsurance = calculateBenefitsSaved(history, coverageDetails, averageConsultationCost);
        Double totalPremiums = monthsActive * monthlyPremium;
        Double ratio = totalSavedByInsurance / (totalPremiums > 0 ? totalPremiums : 1);

        // Generate optimization advice based on usage ratio
        List<String> advice = new ArrayList<>();
        String recommendedPlan = "CURRENT";
        Double potentialSavings = 0.0;

        if (ratio < 0.4) {
            // Low utilization - patient should downgrade plan
            advice.add("Your healthcare consumption is low (Below 40% of your plan value).");
            advice.add("Consider switching to a 'BASIC' plan to reduce monthly premiums.");
            recommendedPlan = "BASIC";
            Double basicMonthlyPremium = getPremiumForPlan("BASIC");
            potentialSavings = Math.max(0, (monthlyPremium - basicMonthlyPremium) * 12);
            log.debug("Low utilization detected - recommended downgrade to BASIC, savings: ${}", potentialSavings);
        } else if (ratio > 1.2) {
            // High utilization - patient should upgrade plan
            advice.add("Your healthcare consumption is high (Above 120% of your plan value).");
            advice.add("Switching to 'INTENSIVE' coverage might reduce your out-of-pocket costs significantly.");
            recommendedPlan = "INTENSIVE";
            Double intensiveMonthlyPremium = getPremiumForPlan("INTENSIVE");
            // Positive difference means upgrade costs more, but covers more
            potentialSavings = Math.max(0, (monthlyPremium - intensiveMonthlyPremium) * 12);
            log.debug("High utilization detected - recommended upgrade to INTENSIVE, additional cost: ${}", -potentialSavings);
        } else {
            // Balanced utilization
            advice.add("Your current plan is well-balanced for your healthcare usage.");
            advice.add("Your insurance coverage effectively matches your medical consumption patterns.");
            recommendedPlan = "CURRENT";
            potentialSavings = 0.0;
            log.debug("Balanced utilization detected - current plan is optimal");
        }

        return SimulationResponseDto.builder()
                .patientId(patientId)
                .currentPlan(formatPlanName(coverageDetails))
                .monthlyPremium(monthlyPremium)
                .totalPremiumsPaid(totalPremiums)
                .totalBenefitsReceived(totalSavedByInsurance)
                .benefitCostRatio(Math.round(ratio * 100.0) / 100.0) // Round to 2 decimals
                .rentabilityStatus(determineRentabilityStatus(ratio))
                .optimizationAdvice(advice)
                .recommendedPlan(recommendedPlan)
                .potentialAnnualSavings(potentialSavings)
                .consultationCount(history.size())
                .estimatedTotalCost(history.size() * averageConsultationCost)
                .build();
    }

    /**
     * Determine rentability status based on benefit/cost ratio
     */
    private String determineRentabilityStatus(Double ratio) {
        if (ratio >= 0.8) {
            return "PROFITABLE";
        } else if (ratio >= 0.4) {
            return "NEUTRAL";
        } else {
            return "LOSS";
        }
    }

    /**
     * Format coverage details to human-readable plan name
     */
    private String formatPlanName(String coverageDetails) {
        if (coverageDetails == null || coverageDetails.isBlank()) {
            return "UNKNOWN";
        }
        
        String normalized = coverageDetails.toUpperCase();
        if (normalized.contains("INTENSIVE") || normalized.contains("100%")) {
            return "INTENSIVE";
        } else if (normalized.contains("COMPREHENSIVE") || normalized.contains("85%")) {
            return "COMPREHENSIVE";
        } else if (normalized.contains("ENHANCED") || normalized.contains("75%")) {
            return "ENHANCED";
        } else {
            return "BASIC";
        }
    }

    /**
     * Get monthly premium based on coverage level
     */
    private Double getPremiumForPlan(String coverageLevel) {
        if (coverageLevel == null) {
            return 120.0; // Default
        }
        
        String normalized = coverageLevel.toUpperCase();
        if (normalized.contains("INTENSIVE") || normalized.contains("100%")) {
            return 200.0;
        } else if (normalized.contains("COMPREHENSIVE") || normalized.contains("85%")) {
            return 150.0;
        } else if (normalized.contains("ENHANCED") || normalized.contains("75%")) {
            return 120.0;
        } else {
            return 80.0; // BASIC
        }
    }

    /**
     * Calculate actual benefits received based on insurance coverage and consultation history
     */
    private Double calculateBenefitsSaved(List<ConsultationResponseDto> consultations, String coverageLevel, Double avgCost) {
        Double coveragePercent = 0.70; // Default
        
        String normalized = coverageLevel.toUpperCase();
        if (normalized.contains("INTENSIVE") || normalized.contains("100%")) {
            coveragePercent = 1.0;
        } else if (normalized.contains("COMPREHENSIVE") || normalized.contains("85%")) {
            coveragePercent = 0.85;
        } else if (normalized.contains("ENHANCED") || normalized.contains("75%")) {
            coveragePercent = 0.75;
        }
        
        return consultations.size() * avgCost * coveragePercent;
    }
}
