package com.neuroguard.riskalertservice.controller;

import com.neuroguard.riskalertservice.dto.AlertRequest;
import com.neuroguard.riskalertservice.dto.AlertResponse;
import com.neuroguard.riskalertservice.service.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider/alerts")
@RequiredArgsConstructor
public class ProviderAlertController {

    private final AlertService alertService;

    @PostMapping("/generate")
    public ResponseEntity<String> triggerGeneration() {
        alertService.generateAlertsForAllPatients();
        return ResponseEntity.ok("Alert generation triggered");
    }

    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(@Valid @RequestBody AlertRequest request,
                                                     HttpServletRequest httpRequest) {
        Long providerId = (Long) httpRequest.getAttribute("userId");
        AlertResponse response = alertService.createAlert(request, providerId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{alertId}")
    public ResponseEntity<AlertResponse> updateAlert(@PathVariable Long alertId,
                                                     @Valid @RequestBody AlertRequest request,
                                                     HttpServletRequest httpRequest) {
        Long providerId = (Long) httpRequest.getAttribute("userId");
        AlertResponse response = alertService.updateAlert(alertId, request, providerId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{alertId}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long alertId,
                                            HttpServletRequest httpRequest) {
        Long providerId = (Long) httpRequest.getAttribute("userId");
        alertService.deleteAlert(alertId, providerId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{alertId}/resolve")
    public ResponseEntity<AlertResponse> resolveAlert(@PathVariable Long alertId,
                                                      HttpServletRequest httpRequest) {
        Long providerId = (Long) httpRequest.getAttribute("userId");
        AlertResponse response = alertService.resolveAlert(alertId, providerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AlertResponse>> getAlertsByPatient(@PathVariable Long patientId) {
        List<AlertResponse> alerts = alertService.getAlertsByPatientId(patientId);
        return ResponseEntity.ok(alerts);
    }
}