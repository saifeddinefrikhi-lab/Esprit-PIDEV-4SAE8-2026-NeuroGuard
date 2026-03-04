package com.neuroguard.riskalertservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlertRequest {
    @NotNull
    private Long patientId;

    @NotBlank
    private String message;

    private String severity; // e.g., "INFO", "WARNING", "CRITICAL"
}