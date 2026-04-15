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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CarePlanServiceClient carePlanServiceClient;

    @Mock
    private SmsService smsService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PrescriptionService prescriptionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Test getPrescriptionById returns prescription when found
     */
    @Test
    void getPrescriptionById_whenFound_returnsPrescription() {
        // Arrange
        setAuth(20L, "ROLE_PROVIDER");
        
        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setPatientId(10L);
        prescription.setProviderId(20L);
        prescription.setContenu("Aspirin 500mg");
        prescription.setDosage("1 tablet daily");
        prescription.setJour("Daily");

        UserDto patient = new UserDto();
        patient.setId(10L);
        patient.setFirstName("John");
        patient.setLastName("Doe");

        UserDto provider = new UserDto();
        provider.setId(20L);
        provider.setFirstName("Dr.");
        provider.setLastName("Smith");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(userServiceClient.getUserById(10L)).thenReturn(patient);
        when(userServiceClient.getUserById(20L)).thenReturn(provider);

        // Act
        PrescriptionResponse result = prescriptionService.getPrescriptionById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Aspirin 500mg", result.getContenu());
        assertEquals("John Doe", result.getPatientName());
    }

    /**
     * Test getPrescriptionById throws exception when not found
     */
    @Test
    void getPrescriptionById_whenNotFound_throwsException() {
        // Arrange
        setAuth(20L, "ROLE_PROVIDER");
        
        when(prescriptionRepository.findById(404L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> prescriptionService.getPrescriptionById(404L));
    }

    /**
     * Test getPrescriptionsList returns provider's prescriptions
     */
    @Test
    void getPrescriptionsList_providerRole_returnsPrescriptionsList() {
        // Arrange
        setAuth(20L, "ROLE_PROVIDER");
        
        Prescription prescription1 = new Prescription();
        prescription1.setId(1L);
        prescription1.setPatientId(10L);
        prescription1.setProviderId(20L);
        prescription1.setContenu("Medication A");

        Prescription prescription2 = new Prescription();
        prescription2.setId(2L);
        prescription2.setPatientId(11L);
        prescription2.setProviderId(20L);
        prescription2.setContenu("Medication B");

        UserDto patient1 = new UserDto();
        patient1.setId(10L);
        patient1.setFirstName("John");
        patient1.setLastName("Doe");

        UserDto patient2 = new UserDto();
        patient2.setId(11L);
        patient2.setFirstName("Jane");
        patient2.setLastName("Smith");

        UserDto provider = new UserDto();
        provider.setId(20L);
        provider.setFirstName("Dr.");
        provider.setLastName("Provider");

        when(prescriptionRepository.findByProviderId(20L))
                .thenReturn(List.of(prescription1, prescription2));
        when(userServiceClient.getUserById(10L)).thenReturn(patient1);
        when(userServiceClient.getUserById(11L)).thenReturn(patient2);
        when(userServiceClient.getUserById(20L)).thenReturn(provider);

        // Act
        List<PrescriptionResponse> result = prescriptionService.getPrescriptionsList();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Medication A", result.get(0).getContenu());
        assertEquals("Medication B", result.get(1).getContenu());
    }

    /**
     * Test getPrescriptionsList returns empty list when no prescriptions
     */
    @Test
    void getPrescriptionsList_noMatch_returnsEmptyList() {
        // Arrange
        setAuth(20L, "ROLE_PROVIDER");
        
        when(prescriptionRepository.findByProviderId(20L)).thenReturn(List.of());

        // Act
        List<PrescriptionResponse> result = prescriptionService.getPrescriptionsList();

        // Assert
        assertEquals(0, result.size());
    }

    /**
     * Test deletePrescription successfully deletes prescription
     */
    @Test
    void deletePrescription_whenFound_deletesSuccessfully() {
        // Arrange
        setAuth(20L, "ROLE_PROVIDER");
        
        Long prescriptionId = 1L;
        Prescription prescription = new Prescription();
        prescription.setId(prescriptionId);
        prescription.setPatientId(10L);
        prescription.setProviderId(20L);

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        // Act
        prescriptionService.deletePrescription(prescriptionId);

        // Assert
        verify(prescriptionRepository).delete(prescription);
    }

    /**
     * Test deletePrescription throws exception when not found
     */
    @Test
    void deletePrescription_whenNotFound_throwsException() {
        // Arrange
        setAuth(20L, "ROLE_PROVIDER");
        
        when(prescriptionRepository.findById(404L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> prescriptionService.deletePrescription(404L));
    }

    /**
     * Helper method to set up security context
     */
    private void setAuth(Long userId, String role) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", "password", authorities);
        auth.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
