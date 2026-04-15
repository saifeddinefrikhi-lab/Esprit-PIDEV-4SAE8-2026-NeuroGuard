package com.esprit.microservice.prescriptionservice.controllers;

import com.esprit.microservice.prescriptionservice.dto.PrescriptionAnalyticsDTO;
import com.esprit.microservice.prescriptionservice.dto.SimpleStatsDTO;
import com.esprit.microservice.prescriptionservice.services.PrescriptionAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prescriptions/analytics")
public class PrescriptionAnalyticsController {

    private final PrescriptionAnalyticsService prescriptionAnalyticsService;

    public PrescriptionAnalyticsController(PrescriptionAnalyticsService prescriptionAnalyticsService) {
        this.prescriptionAnalyticsService = prescriptionAnalyticsService;
    }

    @GetMapping("/global")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ResponseEntity<PrescriptionAnalyticsDTO> getGlobalAnalytics() {
        PrescriptionAnalyticsDTO analytics = prescriptionAnalyticsService.getGlobalAnalytics();
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ResponseEntity<SimpleStatsDTO> getSimpleStats() {
        SimpleStatsDTO stats = prescriptionAnalyticsService.getSimpleStats();
        return ResponseEntity.ok(stats);
    }
}
