package com.neuroguard.riskalertservice.controller;

import com.neuroguard.riskalertservice.dto.AlertResponse;
import com.neuroguard.riskalertservice.service.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}