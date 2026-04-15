package com.esprit.microservice.prescriptionservice.controllers;

import com.esprit.microservice.prescriptionservice.dto.PrescriptionRiskScoreDTO;
import com.esprit.microservice.prescriptionservice.dto.RiskAnalysisReportDTO;
import com.esprit.microservice.prescriptionservice.entities.Prescription;
import com.esprit.microservice.prescriptionservice.repositories.PrescriptionRepository;
import com.esprit.microservice.prescriptionservice.services.RiskScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/prescriptions/risk")
@RequiredArgsConstructor
public class RiskScoreController {

    private final RiskScoreService riskScoreService;
    private final PrescriptionRepository prescriptionRepository;

    /**
     * Get comprehensive risk analysis report for all prescriptions
     * Admin and Providers can see all data
     */
    @GetMapping("/analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ResponseEntity<RiskAnalysisReportDTO> getRiskAnalysis(
            @RequestParam(name = "patientId", required = false) Long patientId) {
        try {
            log.info("Fetching risk analysis report for patientId={}", patientId);
            RiskAnalysisReportDTO report = riskScoreService.generateRiskAnalysisReport(patientId);
            log.info("Risk analysis report generated successfully with {} prescriptions analyzed",
                     report.getTotalAnalyzed());
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating risk analysis report", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get risk score for a specific prescription
     * Patient can see his own prescriptions, providers/admin can see all
     */
    @GetMapping("/{prescriptionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER', 'PATIENT', 'CAREGIVER')")
    public ResponseEntity<PrescriptionRiskScoreDTO> getPrescriptionRiskScore(
            @PathVariable Long prescriptionId) {
        try {
            log.info("Fetching risk score for prescription: {}", prescriptionId);
            Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElse(null);
            
            if (prescription == null) {
                log.warn("Prescription not found: {}", prescriptionId);
                return ResponseEntity.notFound().build();
            }
            
            PrescriptionRiskScoreDTO riskScore = riskScoreService.calculatePrescriptionRiskScore(prescription);
            log.info("Risk score calculated for prescription {}: {}", prescriptionId, riskScore.getOverallRiskScore());
            return ResponseEntity.ok(riskScore);
        } catch (Exception e) {
            log.error("Error calculating risk score for prescription: {}", prescriptionId, e);
            return ResponseEntity.status(500).build();
        }
    }
}
