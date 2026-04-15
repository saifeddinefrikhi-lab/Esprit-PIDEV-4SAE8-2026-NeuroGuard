package com.esprit.microservice.careplanservice.services;

import com.esprit.microservice.careplanservice.dto.PrescriptionRequest;
import com.esprit.microservice.careplanservice.dto.PrescriptionResponse;
import com.esprit.microservice.careplanservice.dto.UserDto;
import com.esprit.microservice.careplanservice.entities.Prescription;
import com.esprit.microservice.careplanservice.exceptions.ResourceNotFoundException;
import com.esprit.microservice.careplanservice.exceptions.UnauthorizedException;
import com.esprit.microservice.careplanservice.feign.UserServiceClient;
import com.esprit.microservice.careplanservice.repositories.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
                .createdAt(prescription.getCreatedAt())
                .updatedAt(prescription.getUpdatedAt())
                .build();
    }

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

        Prescription saved = prescriptionRepository.save(prescription);
        return mapToResponse(saved);
    }

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

        Prescription updated = prescriptionRepository.save(prescription);
        return mapToResponse(updated);
    }

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
        prescriptionRepository.delete(prescription);
    }

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
}
