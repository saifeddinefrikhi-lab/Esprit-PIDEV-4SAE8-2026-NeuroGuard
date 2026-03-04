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
@RequestMapping("/api/patient/alerts")
@RequiredArgsConstructor
public class PatientAlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertResponse>> getMyAlerts(HttpServletRequest request) {
        Long patientId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("userRole");
        List<AlertResponse> alerts = alertService.getAlertsForPatient(patientId, patientId, role);
        return ResponseEntity.ok(alerts);
    }
}