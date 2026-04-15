package com.esprit.microservice.prescriptionservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PrescriptionResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long providerId;
    private String providerName;
    private String contenu;
    private String notes;
    private String jour;
    private String dosage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
