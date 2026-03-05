package com.neuroguard.riskalertservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientFeatures {
    private Long patientId;
    private int age;
    private String gender;
    private String progressionStage;   // MILD, MODERATE, SEVERE
    private int yearsSinceDiagnosis;
    private int comorbidityCount;
    private int allergyCount;
    private boolean hasGeneticRisk;
    private boolean hasFamilyHistory;
    private int surgeryCount;
    private int caregiverCount;
    private int providerCount;
    // Add more as needed
}