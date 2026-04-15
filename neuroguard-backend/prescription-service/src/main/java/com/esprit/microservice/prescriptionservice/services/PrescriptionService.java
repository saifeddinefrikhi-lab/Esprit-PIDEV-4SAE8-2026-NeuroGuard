package com.esprit.microservice.prescriptionservice.services;

import com.esprit.microservice.prescriptionservice.dto.PrescriptionRequest;
import com.esprit.microservice.prescriptionservice.dto.PrescriptionResponse;
import com.esprit.microservice.prescriptionservice.dto.UserDto;
import com.esprit.microservice.prescriptionservice.entities.Prescription;
import com.esprit.microservice.prescriptionservice.exceptions.ResourceNotFoundException;
import com.esprit.microservice.prescriptionservice.exceptions.UnauthorizedException;
import com.esprit.microservice.prescriptionservice.feign.UserServiceClient;
import com.esprit.microservice.prescriptionservice.feign.CarePlanServiceClient;
import com.esprit.microservice.prescriptionservice.repositories.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final UserServiceClient userServiceClient;
    private final CarePlanServiceClient carePlanServiceClient;
    private final SmsService smsService;
    private final SimpMessagingTemplate messagingTemplate;

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
    }

    private String getCurrentUserRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElseThrow();
    }

    private void validatePatient(Long patientId) {
        UserDto patient = userServiceClient.getUserById(patientId);
        if (patient == null || !"PATIENT".equals(patient.getRole())) {
            throw new ResourceNotFoundException("Patient not found or not a patient");
        }
    }

    /**
     * Broadcast prescription notification to patient and all assigned caregivers
     * @param patientId the patient Id to notify
     * @param payload the notification payload (PrescriptionResponse or deletion message)
     */
    private void broadcastPrescriptionNotification(Long patientId, Object payload) {
        try {
            // Notify patient
            messagingTemplate.convertAndSend("/topic/prescriptions/" + patientId, payload);

            // Notify all caregivers assigned to this patient
            List<Long> caregiverIds = userServiceClient.getCaregiverIdsByPatient(patientId);
            for (Long caregiverId : caregiverIds) {
                messagingTemplate.convertAndSend("/topic/prescriptions/" + caregiverId, payload);
            }
            log.info("Prescription notification broadcasted to patient {} and {} caregivers", 
                    patientId, caregiverIds.size());
        } catch (Exception e) {
            log.error("Error broadcasting prescription notification: {}", e.getMessage());
            // Still notify patient even if caregiver notification fails
            messagingTemplate.convertAndSend("/topic/prescriptions/" + patientId, payload);
        }
    }

    private PrescriptionResponse mapToResponse(Prescription prescription) {
        String patientName = null;
        String providerName = null;
        try {
            UserDto patient = userServiceClient.getUserById(prescription.getPatientId());
            if (patient != null) {
                patientName = (patient.getFirstName() != null ? patient.getFirstName() : "") + " "
                        + (patient.getLastName() != null ? patient.getLastName() : "");
                patientName = patientName.trim();
            }
            UserDto provider = userServiceClient.getUserById(prescription.getProviderId());
            if (provider != null) {
                providerName = (provider.getFirstName() != null ? provider.getFirstName() : "") + " "
                        + (provider.getLastName() != null ? provider.getLastName() : "");
                providerName = providerName.trim();
            }
        } catch (Exception e) {
            log.debug("Could not fetch user names: {}", e.getMessage());
        }
        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .patientId(prescription.getPatientId())
                .patientName(patientName)
                .providerId(prescription.getProviderId())
                .providerName(providerName)
                .contenu(prescription.getContenu())
                .notes(prescription.getNotes())
                .jour(prescription.getJour())
                .dosage(prescription.getDosage())
                .createdAt(prescription.getCreatedAt())
                .updatedAt(prescription.getUpdatedAt())
                .build();
    }

    /** Doctor (PROVIDER) or ADMIN: create prescription */
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    @Transactional
    public PrescriptionResponse createPrescription(PrescriptionRequest request) {
        Long providerId = getCurrentUserId();
        if (getCurrentUserRole().equals("ADMIN") && request.getProviderId() != null) {
            providerId = request.getProviderId();
        }
        validatePatient(request.getPatientId());

        Prescription prescription = new Prescription();
        prescription.setPatientId(request.getPatientId());
        prescription.setProviderId(providerId);
        prescription.setContenu(request.getContenu().trim());
        prescription.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        prescription.setJour(request.getJour() != null ? request.getJour().trim() : null);
        prescription.setDosage(request.getDosage() != null ? request.getDosage().trim() : null);

        Prescription saved = prescriptionRepository.save(prescription);
        sendPrescriptionCreatedSmsToPatient(saved);
        PrescriptionResponse response = mapToResponse(saved);

        // Notify patient and caregivers via WebSocket
        broadcastPrescriptionNotification(request.getPatientId(), response);

        return response;
    }

    private void sendPrescriptionCreatedSmsToPatient(Prescription prescription) {
        try {
            UserDto patient = userServiceClient.getUserById(prescription.getPatientId());
            UserDto provider = userServiceClient.getUserById(prescription.getProviderId());

            if (patient == null) {
                log.warn("[SMS] Cannot send SMS: patient id {} not found in user-service.", prescription.getPatientId());
                return;
            }

            String providerLabel = "votre provider";
            if (provider != null) {
                String providerName = ((provider.getFirstName() != null ? provider.getFirstName() : "") + " "
                        + (provider.getLastName() != null ? provider.getLastName() : "")).trim();
                if (!providerName.isBlank()) {
                    providerLabel = providerName;
                }
            }

            String message = "NeuroGuard: Nouvelle prescription #" + prescription.getId()
                    + " ajoutee par " + providerLabel + ".";
            smsService.sendSms(patient.getPhoneNumber(), message);
        } catch (Exception e) {
            log.error("[SMS] Could not prepare SMS for prescription {}: {}", prescription.getId(), e.getMessage(), e);
        }
    }

    /** Doctor (PROVIDER) or ADMIN: update prescription */
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    @Transactional
    public PrescriptionResponse updatePrescription(Long id, PrescriptionRequest request) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));

        if (!getCurrentUserRole().equals("ADMIN")) {
            if (!prescription.getProviderId().equals(getCurrentUserId())) {
                throw new UnauthorizedException("You are not the creator of this prescription");
            }
        }

        if (!prescription.getPatientId().equals(request.getPatientId())) {
            validatePatient(request.getPatientId());
            prescription.setPatientId(request.getPatientId());
        }
        if (getCurrentUserRole().equals("ADMIN") && request.getProviderId() != null) {
            prescription.setProviderId(request.getProviderId());
        }
        prescription.setContenu(request.getContenu().trim());
        prescription.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        prescription.setJour(request.getJour() != null ? request.getJour().trim() : null);
        prescription.setDosage(request.getDosage() != null ? request.getDosage().trim() : null);

        Prescription updated = prescriptionRepository.save(prescription);
        PrescriptionResponse response = mapToResponse(updated);

        // Notify patient and caregivers via WebSocket
        broadcastPrescriptionNotification(request.getPatientId(), response);

        return response;
    }

    /** Doctor (PROVIDER) or ADMIN: delete prescription */
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    @Transactional
    public void deletePrescription(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));

        if (!getCurrentUserRole().equals("ADMIN")) {
            if (!prescription.getProviderId().equals(getCurrentUserId())) {
                throw new UnauthorizedException("You are not the creator of this prescription");
            }
        }
        Long patientId = prescription.getPatientId();
        prescriptionRepository.delete(prescription);

        // Notify patient and caregivers that prescription was deleted via WebSocket
        broadcastPrescriptionNotification(patientId, "DELETED:" + id);
    }

    /**
     * Patient: read only. Get prescription by ID (patient sees own,
     * doctor/caregiver/admin see if allowed).
     */
    @PreAuthorize("isAuthenticated()")
    public PrescriptionResponse getPrescriptionById(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));

        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();

        if ("PROVIDER".equals(role)) {
            return mapToResponse(prescription);
        }
        if ("PATIENT".equals(role)) {
            if (!prescription.getPatientId().equals(userId)) {
                throw new UnauthorizedException("You can only view your own prescriptions");
            }
            return mapToResponse(prescription);
        }
        if ("CAREGIVER".equals(role)) {
            Boolean assigned = userServiceClient.isCaregiverAssignedToPatient(userId, prescription.getPatientId());
            if (Boolean.TRUE.equals(assigned)) {
                return mapToResponse(prescription);
            }
            throw new UnauthorizedException("You are not assigned to this patient");
        }
        if ("ADMIN".equals(role)) {
            return mapToResponse(prescription);
        }
        throw new UnauthorizedException("Access denied");
    }

    // Récupération de l'entité Prescription pour génération PDF (mêmes contrôles d'accès)
    @PreAuthorize("isAuthenticated()")
    public Prescription getPrescriptionEntityById(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));

        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();

        if ("PROVIDER".equals(role)) {
            return prescription;
        }
        if ("PATIENT".equals(role)) {
            if (!prescription.getPatientId().equals(userId)) {
                throw new UnauthorizedException("You can only view your own prescriptions");
            }
            return prescription;
        }
        if ("CAREGIVER".equals(role)) {
            Boolean assigned = userServiceClient.isCaregiverAssignedToPatient(userId, prescription.getPatientId());
            if (Boolean.TRUE.equals(assigned)) {
                return prescription;
            }
            throw new UnauthorizedException("You are not assigned to this patient");
        }
        if ("ADMIN".equals(role)) {
            return prescription;
        }
        throw new UnauthorizedException("Access denied");
    }

    /**
     * List prescriptions for current user by role: doctor=his, admin=all,
     * patient=his, caregiver=assigned patients.
     */
    @PreAuthorize("isAuthenticated()")
    public List<PrescriptionResponse> getPrescriptionsList() {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();

        if ("PROVIDER".equals(role)) {
            return prescriptionRepository.findByProviderId(userId).stream()
                    .map(this::mapToResponse).collect(Collectors.toList());
        }
        if ("ADMIN".equals(role)) {
            return prescriptionRepository.findAll().stream()
                    .map(this::mapToResponse).collect(Collectors.toList());
        }
        if ("PATIENT".equals(role)) {
            return prescriptionRepository.findByPatientId(userId).stream()
                    .map(this::mapToResponse).collect(Collectors.toList());
        }
        if ("CAREGIVER".equals(role)) {
            List<Long> patientIds = userServiceClient.getPatientIdsByCaregiver(userId);
            List<PrescriptionResponse> result = new ArrayList<>();
            for (Long patientId : patientIds) {
                result.addAll(prescriptionRepository.findByPatientId(patientId).stream()
                        .map(this::mapToResponse).collect(Collectors.toList()));
            }
            return result;
        }
        throw new UnauthorizedException("Access denied");
    }

    /**
     * Search prescriptions by keyword (content or notes)
     */
    @PreAuthorize("isAuthenticated()")
    public List<PrescriptionResponse> searchPrescriptions(String keyword) {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();

        List<Prescription> results = new ArrayList<>();
        
        if ("PROVIDER".equals(role)) {
            results = prescriptionRepository.findByProviderId(userId);
        } else if ("PATIENT".equals(role)) {
            results = prescriptionRepository.findByPatientId(userId);
        } else if ("CAREGIVER".equals(role)) {
            List<Long> patientIds = userServiceClient.getPatientIdsByCaregiver(userId);
            results = prescriptionRepository.findAll().stream()
                    .filter(p -> patientIds.contains(p.getPatientId()))
                    .collect(Collectors.toList());
        } else if ("ADMIN".equals(role)) {
            results = prescriptionRepository.findAll();
        }
        
        // Filter by keyword (case-insensitive search in contenu and notes)
        String lowerKeyword = keyword.toLowerCase();
        return results.stream()
                .filter(p -> (p.getContenu() != null && p.getContenu().toLowerCase().contains(lowerKeyword)) || 
                           (p.getNotes() != null && p.getNotes().toLowerCase().contains(lowerKeyword)))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get prescriptions by patient (doctor/admin/caregiver can use; patient only
     * for own patientId).
     */
    @PreAuthorize("isAuthenticated()")
    public List<PrescriptionResponse> getPrescriptionsByPatient(Long patientId) {
        validatePatient(patientId);

        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();

        if ("PATIENT".equals(role) && !userId.equals(patientId)) {
            throw new UnauthorizedException("You can only access your own prescriptions");
        }
        if ("CAREGIVER".equals(role)) {
            Boolean assigned = userServiceClient.isCaregiverAssignedToPatient(userId, patientId);
            if (!Boolean.TRUE.equals(assigned)) {
                throw new UnauthorizedException("You are not assigned to this patient");
            }
        }

        return prescriptionRepository.findByPatientId(patientId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Récupère le PDF d'un plan de soins via le service careplan
     */
    public byte[] getCarePlanPdf(Long carePlanId) {
        try {
            return carePlanServiceClient.getCarePlanPdf(carePlanId);
        } catch (Exception e) {
            log.warn("Failed to fetch care plan PDF for ID: " + carePlanId, e);
            throw new ResourceNotFoundException("Care plan PDF not found");
        }
    }
}

