package com.esprit.microservice.careplanservice.services;

import com.esprit.microservice.careplanservice.dto.PrescriptionRequest;
import com.esprit.microservice.careplanservice.dto.PrescriptionResponse;
import com.esprit.microservice.careplanservice.dto.UserDto;
import com.esprit.microservice.careplanservice.entities.Prescription;
import com.esprit.microservice.careplanservice.exceptions.UnauthorizedException;
import com.esprit.microservice.careplanservice.feign.UserServiceClient;
import com.esprit.microservice.careplanservice.repositories.PrescriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private PrescriptionService prescriptionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPrescription_providerRole_trimsAndSaves() {
        setAuth(7L, "ROLE_PROVIDER");

        PrescriptionRequest request = new PrescriptionRequest();
        request.setPatientId(99L);
        request.setContenu("  take one tablet daily  ");
        request.setNotes("  after lunch  ");

        UserDto patientRole = new UserDto();
        patientRole.setRole("PATIENT");
        when(userServiceClient.getUserById(99L)).thenReturn(patientRole);

        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> {
            Prescription p = invocation.getArgument(0);
            p.setId(123L);
            return p;
        });

        UserDto patient = new UserDto();
        patient.setFirstName("Alice");
        patient.setLastName("Patient");
        UserDto provider = new UserDto();
        provider.setFirstName("Bob");
        provider.setLastName("Provider");

        // Called by mapToResponse after save.
        when(userServiceClient.getUserById(7L)).thenReturn(provider);
        when(userServiceClient.getUserById(99L)).thenReturn(patientRole, patient);

        PrescriptionResponse response = prescriptionService.createPrescription(request);

        assertEquals(123L, response.getId());
        assertEquals(99L, response.getPatientId());
        assertEquals(7L, response.getProviderId());
        assertEquals("take one tablet daily", response.getContenu());
        assertEquals("after lunch", response.getNotes());
    }

    @Test
    void updatePrescription_providerNotCreator_throwsUnauthorized() {
        setAuth(7L, "ROLE_PROVIDER");

        Prescription existing = new Prescription();
        existing.setId(1L);
        existing.setPatientId(99L);
        existing.setProviderId(8L);

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(existing));

        PrescriptionRequest request = new PrescriptionRequest();
        request.setPatientId(99L);
        request.setContenu("new content");

        assertThrows(UnauthorizedException.class, () -> prescriptionService.updatePrescription(1L, request));
    }

    @Test
    void getPrescriptionsByPatient_patientRole_otherPatient_throwsUnauthorized() {
        setAuth(10L, "ROLE_PATIENT");

        UserDto patient = new UserDto();
        patient.setRole("PATIENT");
        when(userServiceClient.getUserById(20L)).thenReturn(patient);

        assertThrows(UnauthorizedException.class, () -> prescriptionService.getPrescriptionsByPatient(20L));

        verify(userServiceClient).getUserById(20L);
    }

    private void setAuth(Long userId, String authority) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("test-user", null, List.of(() -> authority));
        auth.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
