package com.neuroguard.consultationservice.controller;

import com.neuroguard.consultationservice.dto.ConsultationRequest;
import com.neuroguard.consultationservice.dto.ConsultationResponse;
import com.neuroguard.consultationservice.entity.ConsultationType;
import com.neuroguard.consultationservice.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

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
        ConsultationResponse response = consultationService.createConsultation(request, userId, role, false);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Internal endpoint for Reservation Service to create consultations
     * Should NOT require authentication
     */
    @Transactional
    @PostMapping("/internal")
    public ResponseEntity<Map<String, Object>> createConsultationInternal(
            @RequestBody Map<String, Object> request) {
        System.out.println("========== /internal endpoint called ==========");
        System.out.println("Request received:");
        request.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        
        try {
            // Validate required fields
            if (request.get("title") == null || request.get("title").toString().trim().isEmpty()) {
                throw new IllegalArgumentException("title is required");
            }
            if (request.get("startTime") == null || request.get("startTime").toString().trim().isEmpty()) {
                throw new IllegalArgumentException("startTime is required");
            }
            if (request.get("type") == null || request.get("type").toString().trim().isEmpty()) {
                throw new IllegalArgumentException("type is required");
            }
            if (request.get("patientId") == null) {
                throw new IllegalArgumentException("patientId is required");
            }
            if (request.get("providerId") == null) {
                throw new IllegalArgumentException("providerId is required");
            }

            // Parse required fields
            String startTimeStr = request.get("startTime").toString().trim();
            String endTimeStr = request.getOrDefault("endTime", "").toString().trim();
            
            System.out.println("Parsing start time: " + startTimeStr);
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
            
            LocalDateTime endTime = startTime;
            if (!endTimeStr.isEmpty()) {
                System.out.println("Parsing end time: " + endTimeStr);
                endTime = LocalDateTime.parse(endTimeStr);
            } else {
                endTime = startTime.plus(30, ChronoUnit.MINUTES);
            }

            Long patientId = Long.valueOf(request.get("patientId").toString());
            Long providerId = Long.valueOf(request.get("providerId").toString());

            System.out.println("Creating consultation request with:");
            System.out.println("  Title: " + request.get("title"));
            System.out.println("  Start: " + startTime);
            System.out.println("  End: " + endTime);
            System.out.println("  Type: " + request.get("type"));
            System.out.println("  Provider ID: " + providerId);
            System.out.println("  Patient ID: " + patientId);

            // Build ConsultationRequest
            ConsultationRequest consultationRequest = new ConsultationRequest();
            consultationRequest.setTitle(request.get("title").toString());
            consultationRequest.setDescription(request.getOrDefault("description", "").toString());
            consultationRequest.setType(ConsultationType.valueOf(request.get("type").toString()));
            consultationRequest.setStartTime(startTime);
            consultationRequest.setEndTime(endTime);
            consultationRequest.setPatientId(patientId);

            // Call service
            System.out.println("Calling ConsultationService.createConsultation()...");
            ConsultationResponse response = consultationService.createConsultation(
                    consultationRequest, 
                    providerId, 
                    "PROVIDER",
                    true
            );
            
            System.out.println("✓ Consultation created successfully with ID: " + response.getId());

            // Build response
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", response.getId());
            result.put("title", response.getTitle());
            result.put("status", response.getStatus());
            
            System.out.println("========== /internal endpoint returning success ==========");
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
            
        } catch (NumberFormatException nfe) {
            System.err.println("✗ Number format error: " + nfe.getMessage());
            nfe.printStackTrace();
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", "Invalid number format: " + nfe.getMessage());
            error.put("status", "FAILED");
            System.out.println("========== /internal endpoint returning 400 error ==========");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            
        } catch (IllegalArgumentException iae) {
            System.err.println("✗ Validation error: " + iae.getMessage());
            iae.printStackTrace();
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", "Validation error: " + iae.getMessage());
            error.put("status", "FAILED");
            System.out.println("========== /internal endpoint returning 400 error ==========");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            
        } catch (Exception e) {
            System.err.println("✗ Unexpected error: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            error.put("status", "FAILED");
            System.out.println("========== /internal endpoint returning 500 error ==========");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
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

    /**
     * Internal endpoint for other services (like Assurance) to fetch history
     */
    @GetMapping("/all/patient/{patientId}")
    public List<ConsultationResponse> getConsultationsForPatient(@PathVariable Long patientId) {
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

    @GetMapping("/statistics/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminStatistics() {
        return ResponseEntity.ok(consultationService.getGlobalStatistics());
    }

    @GetMapping("/statistics/provider")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<Map<String, Object>> getProviderStatistics(
            @RequestAttribute("userId") Long providerId) {
        return ResponseEntity.ok(consultationService.getProviderStatistics(providerId));
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