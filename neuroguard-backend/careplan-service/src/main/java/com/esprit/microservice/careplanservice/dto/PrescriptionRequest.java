package com.esprit.microservice.careplanservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PrescriptionRequest {
    @NotNull(message = "Patient ID is required")
    @Positive(message = "Patient ID must be positive")
    private Long patientId;

    @Positive(message = "Provider ID must be positive when provided")
    private Long providerId;

    @NotBlank(message = "Prescription content is required")
    private String contenu;

    private String notes;
}
