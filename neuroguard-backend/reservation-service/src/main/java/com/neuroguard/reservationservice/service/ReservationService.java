package com.neuroguard.reservationservice.service;

import com.neuroguard.reservationservice.dto.ReservationDto;
import com.neuroguard.reservationservice.entity.Reservation;
import com.neuroguard.reservationservice.entity.ReservationStatus;
import com.neuroguard.reservationservice.repository.ReservationRepository;
import com.neuroguard.reservationservice.client.UserServiceClient;
import com.neuroguard.reservationservice.client.ConsultationServiceClient;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserServiceClient userServiceClient;
    private final ConsultationServiceClient consultationServiceClient;

    public ReservationService(ReservationRepository reservationRepository, 
                             UserServiceClient userServiceClient,
                             ConsultationServiceClient consultationServiceClient) {
        this.reservationRepository = reservationRepository;
        this.userServiceClient = userServiceClient;
        this.consultationServiceClient = consultationServiceClient;
    }

    @Transactional
    public ReservationDto createReservation(ReservationDto dto) {
        // Check for duplicate reservation
        boolean exists = reservationRepository.existsByProviderIdAndReservationDateAndTimeSlotAndStatusIn(
                dto.getProviderId(),
                dto.getReservationDate(),
                dto.getTimeSlot(),
                List.of(ReservationStatus.PENDING, ReservationStatus.ACCEPTED)
        );

        if (exists) {
            throw new RuntimeException("Ce créneau est déjà réservé ou en attente de confirmation pour ce médecin.");
        }

        Reservation reservation = new Reservation();
        reservation.setPatientId(dto.getPatientId());
        reservation.setProviderId(dto.getProviderId());
        reservation.setReservationDate(dto.getReservationDate());
        reservation.setTimeSlot(dto.getTimeSlot());
        reservation.setConsultationType(dto.getConsultationType());
        reservation.setNotes(dto.getNotes());
        reservation.setStatus(ReservationStatus.PENDING);
        
        Reservation saved = reservationRepository.save(reservation);
        return mapToDto(saved);
    }

    @Transactional
    public ReservationDto updateReservation(Long id, ReservationDto source) {
        return reservationRepository.findById(id).map(reservation -> {
            // Patient can only update their own pending reservations
            if (!ReservationStatus.PENDING.equals(reservation.getStatus())) {
                throw new RuntimeException("Can only update pending reservations");
            }
            
            if (source.getReservationDate() != null) {
                reservation.setReservationDate(source.getReservationDate());
            }
            if (source.getTimeSlot() != null) {
                reservation.setTimeSlot(source.getTimeSlot());
            }
            if (source.getConsultationType() != null) {
                reservation.setConsultationType(source.getConsultationType());
            }
            if (source.getNotes() != null) {
                reservation.setNotes(source.getNotes());
            }
            return mapToDto(reservationRepository.save(reservation));
        }).orElseThrow(() -> new RuntimeException("Reservation not found"));
    }

    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        // Use physical deletion to avoid DB enum/schema mismatch issues on soft-delete status updates.
        reservationRepository.delete(reservation);
    }

    @Transactional
    public ReservationDto acceptReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        
        if (!ReservationStatus.PENDING.equals(reservation.getStatus())) {
            throw new RuntimeException("Only pending reservations can be accepted");
        }

        // Update reservation status to ACCEPTED
        reservation.setStatus(ReservationStatus.ACCEPTED);
        Reservation accepted = reservationRepository.save(reservation);

        // Automatically create consultation
        System.out.println("========== Starting consultation creation process ==========");
        System.out.println("Reservation ID: " + accepted.getId());
        System.out.println("Provider ID: " + accepted.getProviderId());
        System.out.println("Patient ID: " + accepted.getPatientId());
        System.out.println("Reservation Date: " + accepted.getReservationDate());
        System.out.println("Time Slot: " + accepted.getTimeSlot());
        
        try {
            LocalDateTime consultationStart = LocalDateTime.of(
                    accepted.getReservationDate().toLocalDate(),
                    accepted.getTimeSlot()
            );
            LocalDateTime consultationEnd = consultationStart.plus(1, ChronoUnit.HOURS);

            System.out.println("Consultation Start: " + consultationStart);
            System.out.println("Consultation End: " + consultationEnd);

            Map<String, Object> consultationRequest = new HashMap<>();
            consultationRequest.put("title", "Consultation - Patient " + accepted.getPatientId());
            consultationRequest.put("description", "Consultation scheduled from reservation #" + accepted.getId());
            consultationRequest.put("startTime", consultationStart.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            consultationRequest.put("endTime", consultationEnd.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            consultationRequest.put("type", accepted.getConsultationType().toString());
            consultationRequest.put("providerId", accepted.getProviderId());
            consultationRequest.put("patientId", accepted.getPatientId());

            System.out.println("Sending consultation request to service:");
            consultationRequest.forEach((k, v) -> System.out.println("  " + k + ": " + v));
            
            Map<String, Object> consultationResponse = null;
            try {
                consultationResponse = consultationServiceClient.createConsultation(consultationRequest);
                System.out.println("Consultation service response received:");
                if (consultationResponse != null) {
                    consultationResponse.forEach((k, v) -> System.out.println("  " + k + ": " + v));
                } else {
                    System.err.println("Response is null!");
                }
            } catch (IllegalArgumentException iae) {
                System.err.println("Validation error from consultation service: " + iae.getMessage());
                iae.printStackTrace();
                throw iae;
            } catch (RuntimeException re) {
                System.err.println("Runtime error from consultation service: " + re.getMessage());
                re.printStackTrace();
                throw re;
            }
            
            if (consultationResponse != null && consultationResponse.containsKey("id")) {
                try {
                    Long consultationId = Long.valueOf(consultationResponse.get("id").toString());
                    System.out.println("Consultation created with ID: " + consultationId);
                    accepted.setConsultationId(consultationId);
                    reservationRepository.save(accepted);
                    System.out.println("✓ Consultation successfully linked to reservation");
                    System.out.println("========== Consultation creation completed successfully ==========");
                } catch (NumberFormatException nfe) {
                    System.err.println("Error parsing consultation ID: " + nfe.getMessage());
                    throw new RuntimeException("Invalid consultation ID format in response", nfe);
                }
            } else if (consultationResponse != null && consultationResponse.containsKey("error")) {
                String errorMsg = String.valueOf(consultationResponse.get("error"));
                System.err.println("Consultation service returned error: " + errorMsg);
                throw new RuntimeException("Consultation creation failed: " + errorMsg);
            } else {
                System.err.println("Unexpected response format - no ID or error: " + consultationResponse);
                throw new RuntimeException("Invalid response from consultation service");
            }
        } catch (Exception e) {
            System.err.println("✗ Failed to create consultation: " + e.getMessage());
            e.printStackTrace();
            System.out.println("========== Consultation creation failed, but reservation was accepted ==========");
            // Continue with reservation acceptance even if consultation creation fails
            // The UI will inform the user about the partial success/failure
        }

        return mapToDto(accepted);
    }

    @Transactional
    public ReservationDto rejectReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        
        if (!ReservationStatus.PENDING.equals(reservation.getStatus())) {
            throw new RuntimeException("Only pending reservations can be rejected");
        }

        reservation.setStatus(ReservationStatus.REJECTED);
        return mapToDto(reservationRepository.save(reservation));
    }

    public List<ReservationDto> getReservationsByPatient(Long patientId) {
        return reservationRepository.findByPatientIdOrderByReservationDateDesc(patientId)
                .stream()
                .filter(r -> !ReservationStatus.DELETED.equals(r.getStatus()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ReservationDto> getReservationsByProvider(Long providerId) {
        return reservationRepository.findByProviderIdOrderByReservationDateDesc(providerId)
                .stream()
                .filter(r -> !ReservationStatus.DELETED.equals(r.getStatus()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ReservationDto> getReservationsByProviderAndStatus(Long providerId, ReservationStatus status) {
        return reservationRepository.findByProviderIdAndStatusOrderByReservationDateDesc(providerId, status)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ReservationDto> getPendingReservationsForProvider(Long providerId) {
        return getReservationsByProviderAndStatus(providerId, ReservationStatus.PENDING);
    }
    
    public ReservationDto getReservationById(Long id) {
        return reservationRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
    }

    public Map<String, Object> getAdminStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Group by Status
        List<Object[]> statusCounts = reservationRepository.countByStatus();
        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : statusCounts) {
            byStatus.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("byStatus", byStatus);

        // Top Patients (Jointure)
        List<Object[]> topPatientData = reservationRepository.findTopPatientIds();
        List<Map<String, Object>> topPatients = topPatientData.stream()
                .limit(5)
                .map(row -> {
                    Long patientId = (Long) row[0];
                    Long count = (Long) row[1];
                    Map<String, Object> patientInfo = new HashMap<>();
                    patientInfo.put("id", patientId);
                    patientInfo.put("count", count);
                    try {
                        Map<String, Object> user = userServiceClient.getUserById(patientId);
                        if (user != null) {
                            patientInfo.put("name", user.get("firstName") + " " + user.get("lastName"));
                        } else {
                            patientInfo.put("name", "Patient #" + patientId);
                        }
                    } catch (Exception e) {
                        patientInfo.put("name", "Patient #" + patientId);
                    }
                    return patientInfo;
                })
                .collect(Collectors.toList());
        stats.put("topPatients", topPatients);

        stats.put("total", reservationRepository.count());

        return stats;
    }

    private ReservationDto mapToDto(Reservation r) {
        ReservationDto dto = new ReservationDto();
        dto.setId(r.getId());
        dto.setPatientId(r.getPatientId());
        dto.setProviderId(r.getProviderId());
        dto.setReservationDate(r.getReservationDate());
        dto.setTimeSlot(r.getTimeSlot());
        dto.setConsultationType(r.getConsultationType());
        dto.setStatus(r.getStatus());
        dto.setNotes(r.getNotes());
        dto.setConsultationId(r.getConsultationId());
        dto.setCreatedAt(r.getCreatedAt());
        
        try {
            Map<String, Object> patient = userServiceClient.getUserById(r.getPatientId());
            if (patient != null) {
                Object firstName = patient.get("firstName");
                Object lastName = patient.get("lastName");
                if (firstName != null && lastName != null) {
                    dto.setPatientName(firstName.toString() + " " + lastName.toString());
                } else {
                    dto.setPatientName("Patient #" + r.getPatientId());
                }
            }
            
            Map<String, Object> provider = userServiceClient.getUserById(r.getProviderId());
            if (provider != null) {
                Object firstName = provider.get("firstName");
                Object lastName = provider.get("lastName");
                if (firstName != null && lastName != null) {
                    dto.setProviderName(firstName.toString() + " " + lastName.toString());
                } else {
                    dto.setProviderName("Provider #" + r.getProviderId());
                }
            }
        } catch (Exception e) {
            // Log fallback silently if user-service is unavailable or user deleted
            System.out.println("Warning: Could not fetch user details for reservation " + r.getId() + ": " + e.getMessage());
        }
        
        return dto;
    }
}
