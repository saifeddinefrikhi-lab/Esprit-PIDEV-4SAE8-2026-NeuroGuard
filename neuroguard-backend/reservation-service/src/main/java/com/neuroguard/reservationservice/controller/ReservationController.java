package com.neuroguard.reservationservice.controller;

import com.neuroguard.reservationservice.dto.ReservationDto;
import com.neuroguard.reservationservice.service.ReservationService;
import com.neuroguard.reservationservice.entity.ReservationStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Create a new reservation (Patient)
     */
    @PostMapping
    public ResponseEntity<ReservationDto> createReservation(@RequestBody ReservationDto dto) {
        return ResponseEntity.ok(reservationService.createReservation(dto));
    }

    /**
     * Update an existing reservation (Patient - only pending)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReservationDto> updateReservation(@PathVariable Long id, @RequestBody ReservationDto dto) {
        return ResponseEntity.ok(reservationService.updateReservation(id, dto));
    }

    /**
     * Delete a reservation (soft delete - mark as DELETED)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReservation(@PathVariable Long id) {
        try {
            System.out.println("DELETE request for reservation ID: " + id);
            reservationService.deleteReservation(id);
            System.out.println("Reservation " + id + " deleted successfully");
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            System.err.println("Error deleting reservation " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(400).body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Unexpected error deleting reservation " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of("error", "Failed to delete reservation: " + e.getMessage()));
        }
    }

    /**
     * Get patient's reservations
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ReservationDto>> getPatientReservations(@PathVariable Long patientId) {
        return ResponseEntity.ok(reservationService.getReservationsByPatient(patientId));
    }

    /**
     * Get provider's reservations
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ReservationDto>> getProviderReservations(@PathVariable Long providerId) {
        return ResponseEntity.ok(reservationService.getReservationsByProvider(providerId));
    }

    /**
     * Get pending reservations for a provider
     */
    @GetMapping("/provider/{providerId}/pending")
    public ResponseEntity<List<ReservationDto>> getPendingReservationsForProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(reservationService.getPendingReservationsForProvider(providerId));
    }

    /**
     * Get a specific reservation
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDto> getReservationById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    /**
     * Accept a reservation (Provider) - automatically creates consultation
     */
    @PostMapping("/{id}/accept")
    public ResponseEntity<ReservationDto> acceptReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.acceptReservation(id));
    }

    /**
     * Reject a reservation (Provider)
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ReservationDto> rejectReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.rejectReservation(id));
    }

    /**
     * Get admin statistics for the dashboard
     */
    @GetMapping("/statistics/admin")
    public ResponseEntity<java.util.Map<String, Object>> getAdminStatistics() {
        return ResponseEntity.ok(reservationService.getAdminStatistics());
    }
}
