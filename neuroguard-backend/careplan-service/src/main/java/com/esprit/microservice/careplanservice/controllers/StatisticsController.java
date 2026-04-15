package com.esprit.microservice.careplanservice.controllers;

import com.esprit.microservice.careplanservice.dto.CarePlanStatisticsDto;
import com.esprit.microservice.careplanservice.dto.PrescriptionStatisticsDto;
import com.esprit.microservice.careplanservice.dto.StatisticsOverviewDto;
import com.esprit.microservice.careplanservice.services.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Récupère les statistiques complètes (plans de soins et prescriptions)
     */
    @GetMapping("/overview")
    public ResponseEntity<StatisticsOverviewDto> getCompleteStatistics() {
        StatisticsOverviewDto overview = statisticsService.getCompleteStatistics();
        return ResponseEntity.ok(overview);
    }

    /**
     * Récupère les statistiques des plans de soins
     */
    @GetMapping("/careplan")
    public ResponseEntity<List<CarePlanStatisticsDto>> getCarePlanStatistics() {
        List<CarePlanStatisticsDto> stats = statisticsService.getCarePlanStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Récupère les statistiques des prescriptions
     */
    @GetMapping("/prescription")
    public ResponseEntity<List<PrescriptionStatisticsDto>> getPrescriptionStatistics() {
        List<PrescriptionStatisticsDto> stats = statisticsService.getPrescriptionStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Récupère les plans de soins groupés par patient
     */
    @GetMapping("/careplan/by-patient")
    public ResponseEntity<List<Object[]>> getCarePlansPerPatient() {
        List<Object[]> data = statisticsService.getCarePlansPerPatient();
        return ResponseEntity.ok(data);
    }

    /**
     * Récupère les plans de soins groupés par provider
     */
    @GetMapping("/careplan/by-provider")
    public ResponseEntity<List<Object[]>> getCarePlansPerProvider() {
        List<Object[]> data = statisticsService.getCarePlansPerProvider();
        return ResponseEntity.ok(data);
    }

    /**
     * Récupère les prescriptions groupées par patient
     */
    @GetMapping("/prescription/by-patient")
    public ResponseEntity<List<Object[]>> getPrescriptionsPerPatient() {
        List<Object[]> data = statisticsService.getPrescriptionsPerPatient();
        return ResponseEntity.ok(data);
    }

    /**
     * Récupère les prescriptions groupées par provider
     */
    @GetMapping("/prescription/by-provider")
    public ResponseEntity<List<Object[]>> getPrescriptionsPerProvider() {
        List<Object[]> data = statisticsService.getPrescriptionsPerProvider();
        return ResponseEntity.ok(data);
    }

    /**
     * Récupère le nombre de plans de soins pour un patient
     */
    @GetMapping("/careplan/count/patient/{patientId}")
    public ResponseEntity<Long> getCarePlanCountByPatient(@PathVariable Long patientId) {
        Long count = statisticsService.getCarePlanCountByPatient(patientId);
        return ResponseEntity.ok(count);
    }

    /**
     * Récupère le nombre de prescriptions pour un patient
     */
    @GetMapping("/prescription/count/patient/{patientId}")
    public ResponseEntity<Long> getPrescriptionCountByPatient(@PathVariable Long patientId) {
        Long count = statisticsService.getPrescriptionCountByPatient(patientId);
        return ResponseEntity.ok(count);
    }

    /**
     * Récupère le nombre de plans de soins pour un provider
     */
    @GetMapping("/careplan/count/provider/{providerId}")
    public ResponseEntity<Long> getCarePlanCountByProvider(@PathVariable Long providerId) {
        Long count = statisticsService.getCarePlanCountByProvider(providerId);
        return ResponseEntity.ok(count);
    }

    /**
     * Récupère le nombre de prescriptions pour un provider
     */
    @GetMapping("/prescription/count/provider/{providerId}")
    public ResponseEntity<Long> getPrescriptionCountByProvider(@PathVariable Long providerId) {
        Long count = statisticsService.getPrescriptionCountByProvider(providerId);
        return ResponseEntity.ok(count);
    }
}
