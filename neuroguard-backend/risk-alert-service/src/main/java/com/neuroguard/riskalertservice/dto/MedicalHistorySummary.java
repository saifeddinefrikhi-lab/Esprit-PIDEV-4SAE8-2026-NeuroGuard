package com.neuroguard.riskalertservice.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class MedicalHistorySummary {
    private Long patientId;
    private String diagnosis;
    private LocalDate diagnosisDate;
    private String progressionStage;
    private String geneticRisk;
    private String familyHistory;
    private String environmentalFactors;
    private String comorbidities;
    private String medicationAllergies;
    private String environmentalAllergies;
    private String foodAllergies;
    private List<Long> caregiverIds;
    private List<Long> providerIds;
}