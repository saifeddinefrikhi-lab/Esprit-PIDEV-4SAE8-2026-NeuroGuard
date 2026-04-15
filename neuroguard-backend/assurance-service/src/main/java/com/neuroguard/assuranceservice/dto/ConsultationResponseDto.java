package com.neuroguard.assuranceservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsultationResponseDto {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long patientId;
    private Long providerId;
}
