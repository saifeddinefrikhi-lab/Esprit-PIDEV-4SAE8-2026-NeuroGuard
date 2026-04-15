package com.esprit.microservice.prescriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleStatsDTO {
    private Long totalPrescriptions;
    private Long totalDoctors;
    private Long totalPatients;
    private LocalDateTime lastUpdated;
    private Long recentPrescriptions; // Last 7 days
}
