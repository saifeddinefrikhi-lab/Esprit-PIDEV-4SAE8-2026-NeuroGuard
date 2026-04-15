package com.esprit.microservice.careplanservice.services;

import com.esprit.microservice.careplanservice.dto.CarePlanResponse;
import com.esprit.microservice.careplanservice.dto.UserDto;
import com.esprit.microservice.careplanservice.entities.CarePlan;
import com.esprit.microservice.careplanservice.exceptions.UnauthorizedException;
import com.esprit.microservice.careplanservice.feign.UserServiceClient;
import com.esprit.microservice.careplanservice.repositories.CarePlanMessageRepository;
import com.esprit.microservice.careplanservice.repositories.CarePlanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarePlanServiceTest {

    @Mock
    private CarePlanRepository carePlanRepository;

    @Mock
    private CarePlanMessageRepository carePlanMessageRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CarePlanMailService carePlanMailService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CarePlanService carePlanService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCarePlansList_providerRole_returnsOnlyProviderPlans() {
        setAuth(7L, "ROLE_PROVIDER");

        CarePlan plan = new CarePlan();
        plan.setId(1L);
        plan.setProviderId(7L);
        plan.setPatientId(99L);
        when(carePlanRepository.findByProviderId(7L)).thenReturn(List.of(plan));

        UserDto patient = new UserDto();
        patient.setFirstName("Alice");
        patient.setLastName("Patient");
        UserDto provider = new UserDto();
        provider.setFirstName("Bob");
        provider.setLastName("Provider");

        // mapToResponse fetches patient then provider by ID.
        when(userServiceClient.getUserById(99L)).thenReturn(patient);
        when(userServiceClient.getUserById(7L)).thenReturn(provider);

        List<CarePlanResponse> result = carePlanService.getCarePlansList();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Alice Patient", result.get(0).getPatientName());
        verify(carePlanRepository).findByProviderId(7L);
    }

    @Test
    void getCarePlansByPatient_patientRole_otherPatient_throwsUnauthorized() {
        setAuth(10L, "ROLE_PATIENT");

        UserDto targetPatient = new UserDto();
        targetPatient.setId(20L);
        targetPatient.setRole("PATIENT");
        when(userServiceClient.getUserById(20L)).thenReturn(targetPatient);

        assertThrows(UnauthorizedException.class, () -> carePlanService.getCarePlansByPatient(20L));

        verify(userServiceClient).getUserById(20L);
    }

    private void setAuth(Long userId, String authority) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("test-user", null, List.of(() -> authority));
        auth.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
