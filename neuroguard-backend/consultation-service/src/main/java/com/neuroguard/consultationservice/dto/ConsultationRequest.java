package com.neuroguard.consultationservice.dto;

import com.neuroguard.consultationservice.entity.ConsultationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class ConsultationRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @NotNull
    private ConsultationType type;
    @NotNull
    private Long patientId;
    private Long caregiverId;
    /**
     * ID du médecin/infirmier (provider). Obligatoire quand un CAREGIVER crée la consultation.
     * Ignoré quand un PROVIDER crée (on utilise son propre ID).
     */
    private Long providerId;

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public ConsultationType getType() { return type; }
    public void setType(ConsultationType type) { this.type = type; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getCaregiverId() { return caregiverId; }
    public void setCaregiverId(Long caregiverId) { this.caregiverId = caregiverId; }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
}