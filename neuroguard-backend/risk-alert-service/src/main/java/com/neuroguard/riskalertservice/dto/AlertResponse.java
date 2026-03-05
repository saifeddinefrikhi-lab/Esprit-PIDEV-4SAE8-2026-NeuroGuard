package com.neuroguard.riskalertservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlertResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private String message;
    private String severity;
    private boolean resolved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}