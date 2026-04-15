package com.esprit.microservice.careplanservice.controllers;

import com.esprit.microservice.careplanservice.dto.PrescriptionRequest;
import com.esprit.microservice.careplanservice.dto.PrescriptionResponse;
import com.esprit.microservice.careplanservice.dto.PrescriptionStatisticsDto;
import com.esprit.microservice.careplanservice.services.PrescriptionService;
import com.esprit.microservice.careplanservice.services.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/care-plans/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final StatisticsService statisticsService;

    @PostMapping
    public ResponseEntity<PrescriptionResponse> createPrescription(@Valid @RequestBody PrescriptionRequest request) {
        PrescriptionResponse response = prescriptionService.createPrescription(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionResponse> updatePrescription(@PathVariable Long id,
                                                                   @Valid @RequestBody PrescriptionRequest request) {
        PrescriptionResponse response = prescriptionService.updatePrescription(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponse> getPrescriptionById(@PathVariable Long id) {
        PrescriptionResponse response = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsList() {
        List<PrescriptionResponse> responses = prescriptionService.getPrescriptionsList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByPatient(@RequestParam Long patientId) {
        List<PrescriptionResponse> responses = prescriptionService.getPrescriptionsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }

    /** Get prescription statistics grouped by date */
    @GetMapping("/statistics/detailed")
    public ResponseEntity<List<PrescriptionStatisticsDto>> getDetailedStatistics() {
        List<PrescriptionStatisticsDto> stats = statisticsService.getPrescriptionStatistics();
        return ResponseEntity.ok(stats);
    }

    /** Get prescriptions grouped by patient */
    @GetMapping("/statistics/by-patient")
    public ResponseEntity<List<Object[]>> getPrescriptionsPerPatient() {
        List<Object[]> data = statisticsService.getPrescriptionsPerPatient();
        return ResponseEntity.ok(data);
    }

    /** Get prescriptions grouped by provider */
    @GetMapping("/statistics/by-provider")
    public ResponseEntity<List<Object[]>> getPrescriptionsPerProvider() {
        List<Object[]> data = statisticsService.getPrescriptionsPerProvider();
        return ResponseEntity.ok(data);
    }

    /** Get total number of prescriptions for a specific patient */
    @GetMapping("/statistics/count/patient/{patientId}")
    public ResponseEntity<Long> getPrescriptionCountByPatient(@PathVariable Long patientId) {
        Long count = statisticsService.getPrescriptionCountByPatient(patientId);
        return ResponseEntity.ok(count);
    }

    /** Get total number of prescriptions for a specific provider */
    @GetMapping("/statistics/count/provider/{providerId}")
    public ResponseEntity<Long> getPrescriptionCountByProvider(@PathVariable Long providerId) {
        Long count = statisticsService.getPrescriptionCountByProvider(providerId);
        return ResponseEntity.ok(count);
    }
}
