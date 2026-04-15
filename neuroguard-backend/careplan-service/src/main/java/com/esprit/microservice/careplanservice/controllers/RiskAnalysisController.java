package com.esprit.microservice.careplanservice.controllers;

import com.esprit.microservice.careplanservice.dto.PrescriptionRiskScoreDto;
import com.esprit.microservice.careplanservice.dto.RiskAnalysisReportDto;
import com.esprit.microservice.careplanservice.services.RiskAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class RiskAnalysisController {

    private final RiskAnalysisService riskAnalysisService;

    /**
     * Analyse les risques de toutes les prescriptions ou pour un patient spécifique
     * Accessible à : ADMIN, PROVIDER, PATIENT, CAREGIVER
     */
    @GetMapping("/risk/analysis")
    public ResponseEntity<RiskAnalysisReportDto> analyzeRisk(
            @RequestParam(required = false) Long patientId) {
        RiskAnalysisReportDto report = riskAnalysisService.analyzeRisk(patientId);
        return ResponseEntity.ok(report);
    }

    /**
     * Obtient le score de risque pour une prescription spécifique
     * Accessible à : ADMIN, PROVIDER, PATIENT, CAREGIVER
     */
    @GetMapping("/risk/{prescriptionId}")
    public ResponseEntity<PrescriptionRiskScoreDto> getRiskScore(
            @PathVariable Long prescriptionId) {
        PrescriptionRiskScoreDto riskScore = riskAnalysisService.getRiskScoreForPrescription(prescriptionId);
        return ResponseEntity.ok(riskScore);
    }
}
