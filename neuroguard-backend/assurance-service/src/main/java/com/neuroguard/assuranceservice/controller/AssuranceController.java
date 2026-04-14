package com.neuroguard.assuranceservice.controller;

import com.neuroguard.assuranceservice.dto.AssuranceRequestDto;
import com.neuroguard.assuranceservice.dto.AssuranceResponseDto;
import com.neuroguard.assuranceservice.dto.CoverageRiskAssessmentDto;
import com.neuroguard.assuranceservice.dto.StatisticsDto;
import com.neuroguard.assuranceservice.entity.AssuranceStatus;
import com.neuroguard.assuranceservice.entity.CoverageRiskAssessment;
import com.neuroguard.assuranceservice.service.AssuranceService;
import com.neuroguard.assuranceservice.service.CoverageRiskAssessmentService;
import com.neuroguard.assuranceservice.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assurances")
@RequiredArgsConstructor
public class AssuranceController {

    private final AssuranceService assuranceService;
    private final CoverageRiskAssessmentService coverageRiskAssessmentService;
    private final StatisticsService statisticsService;

    // TEMPORARY TEST ENDPOINT - Remove after testing
    @GetMapping("/test/{assuranceId}")
    public ResponseEntity<String> testRiskAssessmentData(@PathVariable Long assuranceId) {
        try {
            CoverageRiskAssessment assessment = coverageRiskAssessmentService.calculateRiskAssessment(assuranceId, 9L);
            String result = String.format("Assurance %d -> Medical Complexity: %d, Alzheimer's Risk: %.2f%%, Est. Cost: $%.0f",
                assuranceId,
                assessment.getMedicalComplexityScore(),
                assessment.getAlzheimersPredictionScore() * 100,
                assessment.getEstimatedAnnualClaimCost());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<AssuranceResponseDto> createAssurance(@Valid @RequestBody AssuranceRequestDto request) {
        return new ResponseEntity<>(assuranceService.createAssurance(request), HttpStatus.CREATED);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AssuranceResponseDto>> getAssurancesByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(assuranceService.getAssurancesByPatient(patientId));
    }

    @GetMapping
    public ResponseEntity<List<AssuranceResponseDto>> getAllAssurances() {
        return ResponseEntity.ok(assuranceService.getAllAssurances());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssuranceResponseDto> getAssuranceById(@PathVariable Long id) {
        return ResponseEntity.ok(assuranceService.getAssuranceById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AssuranceResponseDto> updateAssuranceStatus(
            @PathVariable Long id,
            @RequestParam AssuranceStatus status) {
        return ResponseEntity.ok(assuranceService.updateAssuranceStatus(id, status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssuranceResponseDto> updateAssurance(
            @PathVariable Long id,
            @Valid @RequestBody AssuranceRequestDto request) {
        return ResponseEntity.ok(assuranceService.updateAssurance(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssurance(@PathVariable Long id) {
        assuranceService.deleteAssurance(id);
        return ResponseEntity.noContent().build();
    }

    // ============ Coverage Risk Assessment Endpoints ============

    /**
     * Generate a comprehensive coverage risk assessment for an assurance record
     */
    @PostMapping("/{assuranceId}/risk-assessment")
    public ResponseEntity<CoverageRiskAssessmentDto> generateCoverageAssessment(
            @PathVariable Long assuranceId,
            @RequestParam Long patientId) {
        try {
            System.out.println("DEBUG: generateCoverageAssessment called - assuranceId: " + assuranceId + ", patientId: " + patientId);
            CoverageRiskAssessment assessment = coverageRiskAssessmentService.recalculateRiskAssessment(assuranceId, patientId);
            System.out.println("DEBUG: Successfully generated assessment, returning to client");
            return ResponseEntity.ok(mapToDto(assessment));
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in generateCoverageAssessment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CoverageRiskAssessmentDto()); // Return empty DTO instead of no body
        }
    }

    /**
     * Retrieve an existing risk assessment
     */
    @GetMapping("/{assuranceId}/risk-assessment")
    public ResponseEntity<CoverageRiskAssessmentDto> getRiskAssessment(@PathVariable Long assuranceId) {
        System.out.println("DEBUG: Fetching risk assessment for assuranceId: " + assuranceId);
        CoverageRiskAssessment assessment = coverageRiskAssessmentService.getAssessmentByAssuranceId(assuranceId);
        if (assessment == null) {
            System.out.println("DEBUG: No assessment found for assuranceId: " + assuranceId);
            return ResponseEntity.notFound().build();
        }
        System.out.println("DEBUG: Found assessment - assuranceId: " + assessment.getAssuranceId() + 
                         ", patientId: " + assessment.getPatientId() + 
                         ", complexityScore: " + assessment.getMedicalComplexityScore() +
                         ", estimatedCost: " + assessment.getEstimatedAnnualClaimCost());
        return ResponseEntity.ok(mapToDto(assessment));
    }

    /**
     * Recalculate/refresh an existing risk assessment
     */
    @PutMapping("/{assuranceId}/risk-assessment/refresh")
    public ResponseEntity<CoverageRiskAssessmentDto> refreshRiskAssessment(
            @PathVariable Long assuranceId,
            @RequestParam Long patientId) {
        try {
            CoverageRiskAssessment assessment = coverageRiskAssessmentService.recalculateRiskAssessment(assuranceId, patientId);
            return ResponseEntity.ok(mapToDto(assessment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ Advanced Statistics Endpoints ============

    /**
     * Get comprehensive patient-level statistics
     */
    @GetMapping("/stats/patient/{patientId}")
    public ResponseEntity<StatisticsDto.PatientStatistics> getPatientStatistics(
            @PathVariable Long patientId) {
        try {
            StatisticsDto.PatientStatistics stats = statisticsService.getPatientStatistics(patientId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting patient statistics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get comprehensive assurance-level statistics
     */
    @GetMapping("/stats/assurance/{assuranceId}")
    public ResponseEntity<StatisticsDto.AssuranceStatistics> getAssuranceStatistics(
            @PathVariable Long assuranceId) {
        try {
            StatisticsDto.AssuranceStatistics stats = statisticsService.getAssuranceStatistics(assuranceId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting assurance statistics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Map entity to DTO
     */
    private CoverageRiskAssessmentDto mapToDto(CoverageRiskAssessment assessment) {
        CoverageRiskAssessmentDto dto = new CoverageRiskAssessmentDto();
        dto.setId(assessment.getId());
        dto.setAssuranceId(assessment.getAssuranceId());
        dto.setPatientId(assessment.getPatientId());
        dto.setAlzheimersPredictionScore(assessment.getAlzheimersPredictionScore());
        dto.setAlzheimersPredictionLevel(assessment.getAlzheimersPredictionLevel());
        dto.setActiveAlertCount(assessment.getActiveAlertCount());
        dto.setHighestAlertSeverity(assessment.getHighestAlertSeverity());
        dto.setAlertSeverityRatio(assessment.getAlertSeverityRatio());
        dto.setMedicalComplexityScore(assessment.getMedicalComplexityScore());
        dto.setRecommendedCoverageLevel(assessment.getRecommendedCoverageLevel());
        dto.setEstimatedAnnualClaimCost(assessment.getEstimatedAnnualClaimCost());
        dto.setRecommendedProcedures(assessment.getRecommendedProcedures());
        dto.setRecommendedProviderCount(assessment.getRecommendedProviderCount());
        dto.setNeurologyReferralNeeded(assessment.getNeurologyReferralNeeded());
        dto.setGeriatricAssessmentNeeded(assessment.getGeriatricAssessmentNeeded());
        dto.setLastAssessmentDate(assessment.getLastAssessmentDate());
        dto.setNextRecommendedAssessmentDate(assessment.getNextRecommendedAssessmentDate());
        dto.setRiskStratum(assessment.getRiskStratum());
        dto.setCreatedAt(assessment.getCreatedAt());
        dto.setUpdatedAt(assessment.getUpdatedAt());
        return dto;
    }
}
