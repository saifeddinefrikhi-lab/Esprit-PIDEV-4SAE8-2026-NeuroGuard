package com.neuroguard.riskalertservice.service;

import com.neuroguard.riskalertservice.client.MedicalHistoryClient;
import com.neuroguard.riskalertservice.client.UserServiceClient;
import com.neuroguard.riskalertservice.dto.AlertRequest;
import com.neuroguard.riskalertservice.dto.AlertResponse;
import com.neuroguard.riskalertservice.dto.MedicalHistorySummary;
import com.neuroguard.riskalertservice.dto.UserDto;
import com.neuroguard.riskalertservice.entity.Alert;
import com.neuroguard.riskalertservice.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final MedicalHistoryClient medicalHistoryClient;
    private final UserServiceClient userServiceClient;

    // ------------------- Automatic Generation (scheduled & on-demand) -------------------
    @Transactional
    public void generateAlertsForAllPatients() {
        List<UserDto> patients = userServiceClient.getUsersByRole("PATIENT");
        for (UserDto patient : patients) {
            try {
                MedicalHistorySummary history = medicalHistoryClient.getMedicalHistoryByPatientId(patient.getId());
                generateAlertsForPatient(patient, history);
            } catch (Exception e) {
                log.error("Failed to generate alerts for patient {}: {}", patient.getId(), e.getMessage());
            }
        }
    }

    private void generateAlertsForPatient(UserDto patient, MedicalHistorySummary history) {
        // Rule 1: Severe progression stage → CRITICAL
        if ("SEVERE".equalsIgnoreCase(history.getProgressionStage())) {
            createAutoAlert(patient.getId(), "Progression stage is SEVERE. Immediate attention required.", "CRITICAL");
        }

        // Rule 2: Moderate progression + age > 75 → WARNING (increased fall risk)
        if ("MODERATE".equalsIgnoreCase(history.getProgressionStage()) && patient.getDateOfBirth() != null) {
            int age = Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
            if (age > 75) {
                createAutoAlert(patient.getId(), "Moderate progression and age > 75 – high fall risk.", "WARNING");
            }
        }

        // Rule 3: Any allergy recorded → WARNING
        if (hasAllergies(history)) {
            createAutoAlert(patient.getId(), "Patient has allergies that may require attention.", "WARNING");
        }

        // Rule 4: Comorbidities present → INFO (with details)
        if (history.getComorbidities() != null && !history.getComorbidities().isBlank()) {
            createAutoAlert(patient.getId(), "Comorbidities detected: " + history.getComorbidities(), "INFO");
        }

        // Rule 5: Genetic risk factors → INFO
        if (history.getGeneticRisk() != null && !history.getGeneticRisk().isBlank()) {
            createAutoAlert(patient.getId(), "Genetic risk factors recorded: " + history.getGeneticRisk(), "INFO");
        }

        // Rule 6: Family history of note → INFO
        if (history.getFamilyHistory() != null && !history.getFamilyHistory().isBlank()) {
            createAutoAlert(patient.getId(), "Family history recorded: " + history.getFamilyHistory(), "INFO");
        }

        // Rule 7: Environmental factors → INFO
        if (history.getEnvironmentalFactors() != null && !history.getEnvironmentalFactors().isBlank()) {
            createAutoAlert(patient.getId(), "Environmental factors: " + history.getEnvironmentalFactors(), "INFO");
        }

        // Rule 8: Diagnosis recorded → INFO
        if (history.getDiagnosis() != null && !history.getDiagnosis().isBlank()) {
            createAutoAlert(patient.getId(), "Diagnosis: " + history.getDiagnosis(), "INFO");
        }

        // Rule 9: Diagnosis older than 2 years → suggest re-evaluation
        if (history.getDiagnosisDate() != null) {
            int yearsSinceDiagnosis = Period.between(history.getDiagnosisDate(), LocalDate.now()).getYears();
            if (yearsSinceDiagnosis >= 2) {
                createAutoAlert(patient.getId(), "Diagnosis was " + yearsSinceDiagnosis + " years ago. Consider re-evaluation.", "INFO");
            }
        }

        // Rule 10: Multiple caregivers assigned → coordination needed (optional)
        if (history.getCaregiverIds() != null && history.getCaregiverIds().size() > 2) {
            createAutoAlert(patient.getId(), "Multiple caregivers assigned – ensure consistent communication.", "INFO");
        }
    }

    private boolean hasAllergies(MedicalHistorySummary history) {
        return (history.getMedicationAllergies() != null && !history.getMedicationAllergies().isBlank()) ||
                (history.getEnvironmentalAllergies() != null && !history.getEnvironmentalAllergies().isBlank()) ||
                (history.getFoodAllergies() != null && !history.getFoodAllergies().isBlank());
    }

    private void createAutoAlert(Long patientId, String message, String severity) {
        // Avoid duplicate unresolved alerts for the same patient and message
        if (!alertRepository.existsByPatientIdAndMessageAndResolvedFalse(patientId, message)) {
            Alert alert = new Alert();
            alert.setPatientId(patientId);
            alert.setMessage(message);
            alert.setSeverity(severity);
            alert.setResolved(false);
            alert.setCreatedBy(null); // auto-generated
            alertRepository.save(alert);
            log.info("Auto-generated alert for patient {}: {}", patientId, message);
        }
    }

    // ------------------- Patient View -------------------
    public List<AlertResponse> getAlertsForPatient(Long patientId, Long requesterId, String requesterRole) {
        if (!requesterRole.equals("PATIENT") || !patientId.equals(requesterId)) {
            throw new RuntimeException("Access denied: You can only view your own alerts");
        }
        return alertRepository.findByPatientId(patientId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ------------------- Caregiver View -------------------
    public List<AlertResponse> getAlertsForCaregiverPatients(Long caregiverId) {
        // Fetch patients assigned to this caregiver from medical-history-service
        List<UserDto> assignedPatients = medicalHistoryClient.getAssignedPatientsForCaregiver(); // <- no argument
        List<Long> patientIds = assignedPatients.stream().map(UserDto::getId).collect(Collectors.toList());
        if (patientIds.isEmpty()) {
            return List.of();
        }
        return alertRepository.findByPatientIdIn(patientIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ------------------- Provider Operations -------------------
    @Transactional
    public AlertResponse createAlert(AlertRequest request, Long providerId) {
        // Verify patient exists
        try {
            userServiceClient.getUserById(request.getPatientId());
        } catch (Exception e) {
            throw new RuntimeException("Patient not found with id: " + request.getPatientId());
        }

        Alert alert = new Alert();
        alert.setPatientId(request.getPatientId());
        alert.setMessage(request.getMessage());
        alert.setSeverity(request.getSeverity());
        alert.setResolved(false);
        alert.setCreatedBy(providerId);
        alert = alertRepository.save(alert);
        return mapToResponse(alert);
    }

    @Transactional
    public AlertResponse updateAlert(Long alertId, AlertRequest request, Long providerId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        log.info("Provider {} updating alert {}", providerId, alertId);
        alert.setMessage(request.getMessage());
        alert.setSeverity(request.getSeverity());
        alert = alertRepository.save(alert);
        return mapToResponse(alert);
    }

    @Transactional
    public void deleteAlert(Long alertId, Long providerId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        log.info("Provider {} deleting alert {}", providerId, alertId);
        alertRepository.delete(alert);
    }

    @Transactional
    public AlertResponse resolveAlert(Long alertId, Long providerId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        log.info("Provider {} resolving alert {}", providerId, alertId);
        alert.setResolved(true);
        alert = alertRepository.save(alert);
        return mapToResponse(alert);
    }

    public List<AlertResponse> getAlertsByPatientId(Long patientId) {
        return alertRepository.findByPatientId(patientId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ------------------- Helper -------------------
    private AlertResponse mapToResponse(Alert alert) {
        AlertResponse resp = new AlertResponse();
        resp.setId(alert.getId());
        resp.setPatientId(alert.getPatientId());
        // Fetch patient name from user-service
        try {
            UserDto patient = userServiceClient.getUserById(alert.getPatientId());
            resp.setPatientName(patient.getFirstName() + " " + patient.getLastName());
        } catch (Exception e) {
            resp.setPatientName("Unknown");
            log.error("Failed to fetch patient name for id {}", alert.getPatientId(), e);
        }
        resp.setMessage(alert.getMessage());
        resp.setSeverity(alert.getSeverity());
        resp.setResolved(alert.isResolved());
        resp.setCreatedAt(alert.getCreatedAt());
        resp.setUpdatedAt(alert.getUpdatedAt());
        return resp;
    }
}