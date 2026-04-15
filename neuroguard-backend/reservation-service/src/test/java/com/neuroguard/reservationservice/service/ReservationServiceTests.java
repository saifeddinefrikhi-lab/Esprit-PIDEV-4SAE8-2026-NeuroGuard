package com.neuroguard.reservationservice.service;

import com.neuroguard.reservationservice.client.ConsultationServiceClient;
import com.neuroguard.reservationservice.client.UserServiceClient;
import com.neuroguard.reservationservice.dto.ReservationDto;
import com.neuroguard.reservationservice.entity.ConsultationType;
import com.neuroguard.reservationservice.entity.Reservation;
import com.neuroguard.reservationservice.entity.ReservationStatus;
import com.neuroguard.reservationservice.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService Unit Tests")
class ReservationServiceTests {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ConsultationServiceClient consultationServiceClient;

    @InjectMocks
    private ReservationService reservationService;

    private Reservation testReservation;
    private ReservationDto testDto;
    private Map<String, Object> testPatient;
    private Map<String, Object> testProvider;

    @BeforeEach
    void setUp() {
        // Initialize test patient
        testPatient = new HashMap<>();
        testPatient.put("id", 1L);
        testPatient.put("firstName", "John");
        testPatient.put("lastName", "Doe");
        testPatient.put("email", "john@example.com");

        // Initialize test provider
        testProvider = new HashMap<>();
        testProvider.put("id", 2L);
        testProvider.put("firstName", "Dr.");
        testProvider.put("lastName", "Smith");
        testProvider.put("email", "smith@example.com");

        // Initialize test reservation
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setPatientId(1L);
        testReservation.setProviderId(2L);
        testReservation.setReservationDate(LocalDateTime.now().plusDays(5));
        testReservation.setTimeSlot(LocalTime.of(10, 0));
        testReservation.setConsultationType(ConsultationType.ONLINE);
        testReservation.setStatus(ReservationStatus.PENDING);
        testReservation.setNotes("Initial consultation");
        testReservation.setCreatedAt(LocalDateTime.now());
        testReservation.setUpdatedAt(LocalDateTime.now());

        // Initialize test DTO
        testDto = new ReservationDto();
        testDto.setPatientId(1L);
        testDto.setProviderId(2L);
        testDto.setReservationDate(LocalDateTime.now().plusDays(5));
        testDto.setTimeSlot(LocalTime.of(10, 0));
        testDto.setConsultationType(ConsultationType.ONLINE);
        testDto.setNotes("Initial consultation");
    }

    @Test
    @DisplayName("Should create reservation successfully")
    void testCreateReservation_Success() {
        // Arrange
        when(reservationRepository.existsByProviderIdAndReservationDateAndTimeSlotAndStatusIn(
                anyLong(), any(), any(), any()))
                .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        // Act
        ReservationDto result = reservationService.createReservation(testDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getPatientId());
        assertEquals(2L, result.getProviderId());
        assertEquals(ReservationStatus.PENDING, result.getStatus());
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception for duplicate reservation")
    void testCreateReservation_DuplicateSlot() {
        // Arrange
        when(reservationRepository.existsByProviderIdAndReservationDateAndTimeSlotAndStatusIn(
                anyLong(), any(), any(), any()))
                .thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reservationService.createReservation(testDto));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should update reservation successfully")
    void testUpdateReservation_Success() {
        // Arrange
        ReservationDto updateDto = new ReservationDto();
        updateDto.setReservationDate(LocalDateTime.now().plusDays(10));
        updateDto.setTimeSlot(LocalTime.of(14, 0));

        Reservation updated = new Reservation();
        updated.setId(1L);
        updated.setPatientId(1L);
        updated.setProviderId(2L);
        updated.setReservationDate(LocalDateTime.now().plusDays(10));
        updated.setTimeSlot(LocalTime.of(14, 0));
        updated.setConsultationType(ConsultationType.ONLINE);
        updated.setStatus(ReservationStatus.PENDING);
        updated.setCreatedAt(LocalDateTime.now());
        updated.setUpdatedAt(LocalDateTime.now());

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(updated);

        // Act
        ReservationDto result = reservationService.updateReservation(1L, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(LocalTime.of(14, 0), result.getTimeSlot());
        verify(reservationRepository, times(1)).findById(1L);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-pending reservation")
    void testUpdateReservation_InvalidStatus() {
        // Arrange
        Reservation accepted = new Reservation();
        accepted.setId(1L);
        accepted.setStatus(ReservationStatus.ACCEPTED);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(accepted));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reservationService.updateReservation(1L, testDto));
        verify(reservationRepository, times(1)).findById(1L);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent reservation")
    void testUpdateReservation_NotFound() {
        // Arrange
        when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reservationService.updateReservation(1L, testDto));
        verify(reservationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should delete reservation successfully")
    void testDeleteReservation_Success() {
        // Arrange
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        doNothing().when(reservationRepository).delete(any(Reservation.class));

        // Act
        reservationService.deleteReservation(1L);

        // Assert
        verify(reservationRepository, times(1)).findById(1L);
        verify(reservationRepository, times(1)).delete(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent reservation")
    void testDeleteReservation_NotFound() {
        // Arrange
        when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reservationService.deleteReservation(1L));
        verify(reservationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should accept pending reservation and create consultation")
    void testAcceptReservation_Success() {
        // Arrange
        Map<String, Object> consultationResponse = new HashMap<>();
        consultationResponse.put("id", 100L);
        consultationResponse.put("title", "Consultation");

        Reservation accepted = new Reservation();
        accepted.setId(1L);
        accepted.setPatientId(1L);
        accepted.setProviderId(2L);
        accepted.setReservationDate(LocalDateTime.now().plusDays(5));
        accepted.setTimeSlot(LocalTime.of(10, 0));
        accepted.setConsultationType(ConsultationType.ONLINE);
        accepted.setStatus(ReservationStatus.PENDING);
        accepted.setCreatedAt(LocalDateTime.now());
        accepted.setUpdatedAt(LocalDateTime.now());

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(accepted));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(accepted);
        when(consultationServiceClient.createConsultation(any(Map.class))).thenReturn(consultationResponse);

        // Act
        ReservationDto result = reservationService.acceptReservation(1L);

        // Assert
        assertNotNull(result);
        assertEquals(ReservationStatus.ACCEPTED, result.getStatus());
        verify(reservationRepository, atLeastOnce()).findById(1L);
        verify(reservationRepository, atLeastOnce()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception when accepting non-pending reservation")
    void testAcceptReservation_InvalidStatus() {
        // Arrange
        Reservation rejected = new Reservation();
        rejected.setId(1L);
        rejected.setStatus(ReservationStatus.REJECTED);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(rejected));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reservationService.acceptReservation(1L));
        verify(reservationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when accepting non-existent reservation")
    void testAcceptReservation_NotFound() {
        // Arrange
        when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reservationService.acceptReservation(1L));
        verify(reservationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should handle consultation creation failure gracefully")
    void testAcceptReservation_ConsultationCreationFails() {
        // Arrange
        Reservation pending = new Reservation();
        pending.setId(1L);
        pending.setPatientId(1L);
        pending.setProviderId(2L);
        pending.setReservationDate(LocalDateTime.now().plusDays(5));
        pending.setTimeSlot(LocalTime.of(10, 0));
        pending.setConsultationType(ConsultationType.ONLINE);
        pending.setStatus(ReservationStatus.PENDING);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(pending);
        when(consultationServiceClient.createConsultation(any(Map.class)))
                .thenThrow(new RuntimeException("Consultation service unavailable"));

        // Act - should still accept the reservation
        ReservationDto result = reservationService.acceptReservation(1L);

        // Assert
        assertNotNull(result);
        assertEquals(ReservationStatus.ACCEPTED, result.getStatus());
    }

    @Test
    @DisplayName("Should reject pending reservation successfully")
    void testRejectReservation_Success() {
        // Arrange
        Reservation rejected = new Reservation();
        rejected.setId(1L);
        rejected.setPatientId(1L);
        rejected.setProviderId(2L);
        rejected.setStatus(ReservationStatus.REJECTED);
        rejected.setCreatedAt(LocalDateTime.now());

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(rejected);

        // Act
        ReservationDto result = reservationService.rejectReservation(1L);

        // Assert
        assertNotNull(result);
        assertEquals(ReservationStatus.REJECTED, result.getStatus());
        verify(reservationRepository, times(1)).findById(1L);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception when rejecting non-pending reservation")
    void testRejectReservation_InvalidStatus() {
        // Arrange
        Reservation accepted = new Reservation();
        accepted.setId(1L);
        accepted.setStatus(ReservationStatus.ACCEPTED);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(accepted));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reservationService.rejectReservation(1L));
        verify(reservationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when rejecting non-existent reservation")
    void testRejectReservation_NotFound() {
        // Arrange
        when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reservationService.rejectReservation(1L));
        verify(reservationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should retrieve reservations by patient")
    void testGetReservationsByPatient_Success() {
        // Arrange
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(testReservation);
        when(reservationRepository.findByPatientIdOrderByReservationDateDesc(1L)).thenReturn(reservations);
        when(userServiceClient.getUserById(1L)).thenReturn(testPatient);
        when(userServiceClient.getUserById(2L)).thenReturn(testProvider);

        // Act
        List<ReservationDto> results = reservationService.getReservationsByPatient(1L);

        // Assert
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getPatientId());
        verify(reservationRepository, times(1)).findByPatientIdOrderByReservationDateDesc(1L);
    }

    @Test
    @DisplayName("Should return empty list when patient has no reservations")
    void testGetReservationsByPatient_Empty() {
        // Arrange
        List<Reservation> reservations = new ArrayList<>();
        when(reservationRepository.findByPatientIdOrderByReservationDateDesc(1L)).thenReturn(reservations);

        // Act
        List<ReservationDto> results = reservationService.getReservationsByPatient(1L);

        // Assert
        assertEquals(0, results.size());
        verify(reservationRepository, times(1)).findByPatientIdOrderByReservationDateDesc(1L);
    }

    @Test
    @DisplayName("Should filter deleted reservations when retrieving by patient")
    void testGetReservationsByPatient_FilterDeleted() {
        // Arrange
        Reservation deleted = new Reservation();
        deleted.setId(2L);
        deleted.setPatientId(1L);
        deleted.setStatus(ReservationStatus.DELETED);

        List<Reservation> reservations = new ArrayList<>();
        reservations.add(testReservation);
        reservations.add(deleted);

        when(reservationRepository.findByPatientIdOrderByReservationDateDesc(1L)).thenReturn(reservations);
        when(userServiceClient.getUserById(anyLong())).thenReturn(testPatient);

        // Act
        List<ReservationDto> results = reservationService.getReservationsByPatient(1L);

        // Assert
        assertEquals(1, results.size());
        assertEquals(ReservationStatus.PENDING, results.get(0).getStatus());
    }

    @Test
    @DisplayName("Should retrieve reservations by provider")
    void testGetReservationsByProvider_Success() {
        // Arrange
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(testReservation);
        when(reservationRepository.findByProviderIdOrderByReservationDateDesc(2L)).thenReturn(reservations);
        when(userServiceClient.getUserById(1L)).thenReturn(testPatient);
        when(userServiceClient.getUserById(2L)).thenReturn(testProvider);

        // Act
        List<ReservationDto> results = reservationService.getReservationsByProvider(2L);

        // Assert
        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getProviderId());
        verify(reservationRepository, times(1)).findByProviderIdOrderByReservationDateDesc(2L);
    }

    @Test
    @DisplayName("Should return empty list when provider has no reservations")
    void testGetReservationsByProvider_Empty() {
        // Arrange
        List<Reservation> reservations = new ArrayList<>();
        when(reservationRepository.findByProviderIdOrderByReservationDateDesc(2L)).thenReturn(reservations);

        // Act
        List<ReservationDto> results = reservationService.getReservationsByProvider(2L);

        // Assert
        assertEquals(0, results.size());
        verify(reservationRepository, times(1)).findByProviderIdOrderByReservationDateDesc(2L);
    }

    @Test
    @DisplayName("Should retrieve reservations by provider and status")
    void testGetReservationsByProviderAndStatus_Success() {
        // Arrange
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(testReservation);
        when(reservationRepository.findByProviderIdAndStatusOrderByReservationDateDesc(2L, ReservationStatus.PENDING))
                .thenReturn(reservations);
        when(userServiceClient.getUserById(anyLong())).thenReturn(testPatient);

        // Act
        List<ReservationDto> results = reservationService.getReservationsByProviderAndStatus(2L, ReservationStatus.PENDING);

        // Assert
        assertEquals(1, results.size());
        assertEquals(ReservationStatus.PENDING, results.get(0).getStatus());
    }

    @Test
    @DisplayName("Should get pending reservations for provider")
    void testGetPendingReservationsForProvider_Success() {
        // Arrange
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(testReservation);
        when(reservationRepository.findByProviderIdAndStatusOrderByReservationDateDesc(2L, ReservationStatus.PENDING))
                .thenReturn(reservations);
        when(userServiceClient.getUserById(anyLong())).thenReturn(testPatient);

        // Act
        List<ReservationDto> results = reservationService.getPendingReservationsForProvider(2L);

        // Assert
        assertEquals(1, results.size());
        assertEquals(ReservationStatus.PENDING, results.get(0).getStatus());
    }

    @Test
    @DisplayName("Should retrieve reservation by ID")
    void testGetReservationById_Success() {
        // Arrange
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(userServiceClient.getUserById(1L)).thenReturn(testPatient);
        when(userServiceClient.getUserById(2L)).thenReturn(testProvider);

        // Act
        ReservationDto result = reservationService.getReservationById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(reservationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent reservation")
    void testGetReservationById_NotFound() {
        // Arrange
        when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reservationService.getReservationById(1L));
        verify(reservationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should get admin statistics")
    void testGetAdminStatistics_Success() {
        // Arrange
        List<Object[]> statusCounts = new ArrayList<>();
        statusCounts.add(new Object[]{ReservationStatus.PENDING, 5L});
        statusCounts.add(new Object[]{ReservationStatus.ACCEPTED, 10L});
        statusCounts.add(new Object[]{ReservationStatus.REJECTED, 2L});

        List<Object[]> topPatients = new ArrayList<>();
        topPatients.add(new Object[]{1L, 15L});
        topPatients.add(new Object[]{2L, 12L});

        when(reservationRepository.countByStatus()).thenReturn(statusCounts);
        when(reservationRepository.findTopPatientIds()).thenReturn(topPatients);
        when(reservationRepository.count()).thenReturn(20L);
        when(userServiceClient.getUserById(anyLong())).thenReturn(testPatient);

        // Act
        Map<String, Object> stats = reservationService.getAdminStatistics();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("byStatus"));
        assertTrue(stats.containsKey("topPatients"));
        assertTrue(stats.containsKey("total"));
        assertEquals(20L, stats.get("total"));
        verify(reservationRepository, times(1)).countByStatus();
        verify(reservationRepository, times(1)).findTopPatientIds();
        verify(reservationRepository, times(1)).count();
    }

    @Test
    @DisplayName("Should handle user service unavailable in statistics")
    void testGetAdminStatistics_UserServiceUnavailable() {
        // Arrange
        List<Object[]> statusCounts = new ArrayList<>();
        statusCounts.add(new Object[]{ReservationStatus.PENDING, 5L});

        List<Object[]> topPatients = new ArrayList<>();
        topPatients.add(new Object[]{1L, 15L});

        when(reservationRepository.countByStatus()).thenReturn(statusCounts);
        when(reservationRepository.findTopPatientIds()).thenReturn(topPatients);
        when(reservationRepository.count()).thenReturn(20L);
        when(userServiceClient.getUserById(anyLong())).thenThrow(new RuntimeException("Service unavailable"));

        // Act
        Map<String, Object> stats = reservationService.getAdminStatistics();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("byStatus"));
        assertTrue(stats.containsKey("topPatients"));
        List<Map<String, Object>> patients = (List<Map<String, Object>>) stats.get("topPatients");
        assertFalse(patients.isEmpty());
    }
}
