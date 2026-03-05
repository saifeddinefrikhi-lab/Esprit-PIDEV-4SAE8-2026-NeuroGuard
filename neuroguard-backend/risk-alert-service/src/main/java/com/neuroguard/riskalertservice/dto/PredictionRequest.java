package com.neuroguard.riskalertservice.dto;

import lombok.Data;

@Data
public class PredictionRequest {
    private Long patientId;
    private int age;
    private String gender;
    private String progressionStage;
    private int yearsSinceDiagnosis;
    private int comorbidityCount;
    private int allergyCount;
    private boolean hasGeneticRisk;
    private boolean hasFamilyHistory;
    private int surgeryCount;
    private int caregiverCount;
    private int providerCount;
}