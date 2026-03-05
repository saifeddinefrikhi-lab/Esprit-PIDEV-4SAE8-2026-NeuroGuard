package com.neuroguard.consultationservice.controller;

import com.neuroguard.consultationservice.dto.ConsultationRequest;
import com.neuroguard.consultationservice.dto.ConsultationResponse;
import com.neuroguard.consultationservice.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROVIDER', 'CAREGIVER')")
    public ResponseEntity<ConsultationResponse> create(
            @Valid @RequestBody ConsultationRequest request,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("userRole") String role) {
        ConsultationResponse response = consultationService.createConsultation(request, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ConsultationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ConsultationRequest request,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("userRole") String role) {
        ConsultationResponse response = consultationService.updateConsultation(id, request, userId, role);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("userRole") String role) {
        consultationService.deleteConsultation(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/provider")
    @PreAuthorize("hasRole('PROVIDER')")
    public List<ConsultationResponse> getMyConsultationsAsProvider(
            @RequestAttribute("userId") Long providerId) {
        return consultationService.getConsultationsByProvider(providerId);
    }

    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public List<ConsultationResponse> getMyConsultationsAsPatient(
            @RequestAttribute("userId") Long patientId) {
        return consultationService.getConsultationsByPatient(patientId);
    }

    @GetMapping("/caregiver")
    @PreAuthorize("hasRole('CAREGIVER')")
    public List<ConsultationResponse> getMyConsultationsAsCaregiver(
            @RequestAttribute("userId") Long caregiverId) {
        return consultationService.getConsultationsByCaregiver(caregiverId);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ConsultationResponse> getAllConsultations() {
        return consultationService.getAllConsultations();
    }

    @GetMapping("/{id}/join")
    @PreAuthorize("hasAnyRole('PROVIDER', 'PATIENT', 'CAREGIVER')")
    public ResponseEntity<String> getJoinLink(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("userRole") String role) {
        String link = consultationService.getJoinLink(id, userId, role);
        return ResponseEntity.ok(link);
    }
}