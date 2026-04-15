package com.neuroguard.assuranceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponseDto {
    private Long patientId;
    private String currentPlan;
    private Double monthlyPremium;
    
    // Rentability Section
    private Double totalPremiumsPaid;
    private Double totalBenefitsReceived;
    private Double benefitCostRatio;
    private String rentabilityStatus; // PROFITABLE | NEUTRAL | LOSS
    
    // Optimization Section
    private List<String> optimizationAdvice;
    private String recommendedPlan;
    private Double potentialAnnualSavings;
    
    // Recent Consumption
    private Integer consultationCount;
    private Double estimatedTotalCost;
}
