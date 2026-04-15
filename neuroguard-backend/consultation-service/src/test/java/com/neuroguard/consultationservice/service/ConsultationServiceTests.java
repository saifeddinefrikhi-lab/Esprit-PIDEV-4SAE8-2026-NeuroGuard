package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.ConsultationRequest;
import com.neuroguard.consultationservice.dto.ConsultationResponse;
import com.neuroguard.consultationservice.entity.Consultation;
import com.neuroguard.consultationservice.entity.ConsultationStatus;
import com.neuroguard.consultationservice.entity.ConsultationType;
import com.neuroguard.consultationservice.exception.ResourceNotFoundException;
import com.neuroguard.consultationservice.exception.UnauthorizedException;
import com.neuroguard.consultationservice.repository.ConsultationRepository;
import com.neuroguard.consultationservice.repository.ProviderAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultationService Unit Tests")
class ConsultationServiceTests {

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private ProviderAvailabilityRepository availabilityRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ZoomService zoomService;

    @InjectMocks
    private ConsultationService consultationService;

    private ConsultationRequest validRequest;
    private Consultation savedConsultation;

    @BeforeEach
    void setUp() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        validRequest = new ConsultationRequest();
        validRequest.setTitle("Consultation Test");
        validRequest.setDescription("Test Description");
        validRequest.setStartTime(startTime);
        validRequest.setEndTime(endTime);
        validRequest.setType(ConsultationType.PRESENTIAL);
        validRequest.setProviderId(1L);
        validRequest.setPatientId(2L);

        savedConsultation = new Consultation();
        savedConsultation.setId(1L);
        savedConsultation.setTitle(validRequest.getTitle());
        savedConsultation.setDescription(validRequest.getDescription());
        savedConsultation.setStartTime(startTime);
        savedConsultation.setEndTime(endTime);
        savedConsultation.setType(ConsultationType.PRESENTIAL);
        savedConsultation.setProviderId(1L);
        savedConsultation.setPatientId(2L);
        savedConsultation.setStatus(ConsultationStatus.SCHEDULED);
        savedConsultation.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should retrieve consultations by patient")
    void testGetConsultationsByPatient_Success() {
        // Arrange
        List<Consultation> consultations = new ArrayList<>();
        consultations.add(savedConsultation);
        when(consultationRepository.findByPatientId(2L)).thenReturn(consultations);

        // Act
        List<ConsultationResponse> responses = consultationService.getConsultationsByPatient(2L);

        // Assert
        assertEquals(1, responses.size());
        assertEquals("Consultation Test", responses.get(0).getTitle());
        verify(consultationRepository, times(1)).findByPatientId(2L);
    }

    @Test
    @DisplayName("Should return empty list when patient has null ID")
    void testGetConsultationsByPatient_NullId() {
        // Act
        List<ConsultationResponse> responses = consultationService.getConsultationsByPatient(null);

        // Assert
        assertEquals(0, responses.size());
    }

    @Test
    @DisplayName("Should retrieve consultations by provider")
    void testGetConsultationsByProvider_Success() {
        // Arrange
        List<Consultation> consultations = new ArrayList<>();
        consultations.add(savedConsultation);
        when(consultationRepository.findByProviderId(1L)).thenReturn(consultations);

        // Act
        List<ConsultationResponse> responses = consultationService.getConsultationsByProvider(1L);

        // Assert
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getProviderId());
        verify(consultationRepository, times(1)).findByProviderId(1L);
    }

    @Test
    @DisplayName("Should return empty list when provider has null ID")
    void testGetConsultationsByProvider_NullId() {
        // Act
        List<ConsultationResponse> responses = consultationService.getConsultationsByProvider(null);

        // Assert
        assertEquals(0, responses.size());
    }

    @Test
    @DisplayName("Should retrieve all consultations")
    void testGetAllConsultations_Success() {
        // Arrange
        List<Consultation> consultations = new ArrayList<>();
        consultations.add(savedConsultation);
        when(consultationRepository.findAll()).thenReturn(consultations);

        // Act
        List<ConsultationResponse> responses = consultationService.getAllConsultations();

        // Assert
        assertEquals(1, responses.size());
        verify(consultationRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no consultations exist")
    void testGetAllConsultations_Empty() {
        // Arrange
        when(consultationRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<ConsultationResponse> responses = consultationService.getAllConsultations();

        // Assert
        assertEquals(0, responses.size());
    }

    @Test
    @DisplayName("Should delete consultation successfully")
    void testDeleteConsultation_Success() {
        // Arrange
        when(consultationRepository.findById(1L)).thenReturn(Optional.of(savedConsultation));
        doNothing().when(consultationRepository).delete(any(Consultation.class));

        // Act
        consultationService.deleteConsultation(1L, 1L, "PROVIDER");

        // Assert
        verify(consultationRepository, times(1)).findById(1L);
        verify(consultationRepository, times(1)).delete(savedConsultation);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent consultation")
    void testDeleteConsultation_NotFound() {
        // Arrange
        when(consultationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> consultationService.deleteConsultation(999L, 1L, "PROVIDER"),
            "Should throw ResourceNotFoundException");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException for unauthorized deletion")
    void testDeleteConsultation_Unauthorized() {
        // Arrange
        when(consultationRepository.findById(1L)).thenReturn(Optional.of(savedConsultation));

        // Act & Assert
        assertThrows(UnauthorizedException.class,
            () -> consultationService.deleteConsultation(1L, 999L, "PROVIDER"),
            "Should prevent different provider from deleting consultation");
    }

    @Test
    @DisplayName("Should retrieve caregiver's consultations")
    void testGetConsultationsByCaregiver_Success() {
        // Arrange
        savedConsultation.setCaregiverId(3L);
        List<Consultation> consultations = new ArrayList<>();
        consultations.add(savedConsultation);
        when(consultationRepository.findByCaregiverId(3L)).thenReturn(consultations);

        // Act
        List<ConsultationResponse> responses = consultationService.getConsultationsByCaregiver(3L);

        // Assert
        assertEquals(1, responses.size());
        assertEquals(3L, responses.get(0).getCaregiverId());
        verify(consultationRepository, times(1)).findByCaregiverId(3L);
    }

    @Test
    @DisplayName("Should return empty list when caregiver has null ID")
    void testGetConsultationsByCaregiver_NullId() {
        // Act
        List<ConsultationResponse> responses = consultationService.getConsultationsByCaregiver(null);

        // Assert
        assertEquals(0, responses.size());
    }

    @Test
    @DisplayName("Should handle null PatientId in request")
    void testGetConsultationsByPatient_NullFromFilter() {
        // Act
        List<ConsultationResponse> responses = consultationService.getConsultationsByPatient(null);

        // Assert
        assertEquals(0, responses.size());
        verify(consultationRepository, never()).findByPatientId(any());
    }

    @Test
    @DisplayName("Should get join link for online consultation")
    void testGetJoinLink_Success() {
        // Arrange
        savedConsultation.setType(ConsultationType.ONLINE);
        savedConsultation.setMeetingLink("https://meet.jit.si/NeuroGuard-Consultation-1");
        when(consultationRepository.findById(1L)).thenReturn(Optional.of(savedConsultation));

        // Act
        String joinLink = consultationService.getJoinLink(1L, 1L, "PROVIDER");

        // Assert
        assertNotNull(joinLink);
        assertTrue(joinLink.contains("meet.jit.si"), "Join link should contain Jitsi URL");
        verify(consultationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when getting join link for non-existent consultation")
    void testGetJoinLink_NotFound() {
        // Arrange
        when(consultationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> consultationService.getJoinLink(999L, 1L, "PROVIDER"),
            "Should throw ResourceNotFoundException");
    }

    @Test
    @DisplayName("Should prevent unauthorized access to join link")
    void testGetJoinLink_Unauthorized_Provider() {
        // Arrange
        when(consultationRepository.findById(1L)).thenReturn(Optional.of(savedConsultation));

        // Act & Assert
        assertThrows(UnauthorizedException.class,
            () -> consultationService.getJoinLink(1L, 999L, "PROVIDER"),
            "Should prevent different provider from accessing join link");
    }
}
