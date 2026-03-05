package com.neuroguard.riskalertservice.controller;

import com.neuroguard.riskalertservice.dto.AlertResponse;
import com.neuroguard.riskalertservice.service.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caregiver/alerts")
@RequiredArgsConstructor
public class CaregiverAlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertResponse>> getAssignedPatientsAlerts(HttpServletRequest request) {
        Long caregiverId = (Long) request.getAttribute("userId");
        List<AlertResponse> alerts = alertService.getAlertsForCaregiverPatients(caregiverId);
        return ResponseEntity.ok(alerts);
    }
    @PatchMapping("/{alertId}/resolve")
    public ResponseEntity<AlertResponse> resolveAlert(@PathVariable Long alertId,
                                                      HttpServletRequest httpRequest) {
        Long providerId = (Long) httpRequest.getAttribute("userId");
        AlertResponse response = alertService.resolveAlert(alertId, providerId);
        return ResponseEntity.ok(response);
    }
}