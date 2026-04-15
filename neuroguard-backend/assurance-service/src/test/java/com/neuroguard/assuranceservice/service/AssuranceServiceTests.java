package com.neuroguard.assuranceservice.service;

import com.neuroguard.assuranceservice.dto.AssuranceRequestDto;
import com.neuroguard.assuranceservice.dto.AssuranceResponseDto;
import com.neuroguard.assuranceservice.dto.UserDto;
import com.neuroguard.assuranceservice.entity.Assurance;
import com.neuroguard.assuranceservice.entity.AssuranceStatus;
import com.neuroguard.assuranceservice.repository.AssuranceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssuranceService Unit Tests")
class AssuranceServiceTests {

    @Mock
    private AssuranceRepository assuranceRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private AssuranceServiceImpl assuranceService;

    private Assurance testAssurance;
    private UserDto testUser;
    private AssuranceRequestDto testRequest;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testUser = new UserDto();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john@example.com");

        testRequest = new AssuranceRequestDto();
        testRequest.setPatientId(1L);
        testRequest.setProviderName("Health Insurance Corp");
        testRequest.setPolicyNumber("POL-12345-67890");
        testRequest.setCoverageDetails("Comprehensive coverage for Alzheimer's disease");
        testRequest.setIllness("Alzheimer's Disease");
        testRequest.setPostalCode("75001");
        testRequest.setMobilePhone("+33612345678");

        testAssurance = new Assurance();
        testAssurance.setId(1L);
        testAssurance.setPatientId(1L);
        testAssurance.setProviderName("Health Insurance Corp");
        testAssurance.setPolicyNumber("POL-12345-67890");
        testAssurance.setCoverageDetails("Comprehensive coverage for Alzheimer's disease");
        testAssurance.setIllness("Alzheimer's Disease");
        testAssurance.setPostalCode("75001");
        testAssurance.setMobilePhone("+33612345678");
        testAssurance.setStatus(AssuranceStatus.PENDING);
        testAssurance.setCreatedAt(LocalDateTime.now());
        testAssurance.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create assurance successfully")
    void testCreateAssurance_Success() {
        // Arrange
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(assuranceRepository.save(any(Assurance.class))).thenReturn(testAssurance);

        // Act
        AssuranceResponseDto response = assuranceService.createAssurance(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Health Insurance Corp", response.getProviderName());
        assertEquals(AssuranceStatus.PENDING, response.getStatus());
        verify(assuranceRepository, times(1)).save(any(Assurance.class));
        verify(userServiceClient, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("Should throw exception when patient not found during creation")
    void testCreateAssurance_PatientNotFound() {
        // Arrange
        when(userServiceClient.getUserById(1L)).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> assuranceService.createAssurance(testRequest));
        verify(assuranceRepository, never()).save(any(Assurance.class));
    }

    @Test
    @DisplayName("Should retrieve assurances by patient")
    void testGetAssurancesByPatient_Success() {
        // Arrange
        List<Assurance> assurances = new ArrayList<>();
        assurances.add(testAssurance);
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(assuranceRepository.findByPatientId(1L)).thenReturn(assurances);

        // Act
        List<AssuranceResponseDto> responses = assuranceService.getAssurancesByPatient(1L);

        // Assert
        assertEquals(1, responses.size());
        assertEquals("Health Insurance Corp", responses.get(0).getProviderName());
        verify(assuranceRepository, times(1)).findByPatientId(1L);
        verify(userServiceClient, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("Should return empty list when patient has no assurances")
    void testGetAssurancesByPatient_Empty() {
        // Arrange
        List<Assurance> assurances = new ArrayList<>();
        when(assuranceRepository.findByPatientId(1L)).thenReturn(assurances);

        // Act
        List<AssuranceResponseDto> responses = assuranceService.getAssurancesByPatient(1L);

        // Assert
        assertEquals(0, responses.size());
        verify(assuranceRepository, times(1)).findByPatientId(1L);
    }

    @Test
    @DisplayName("Should handle user service unavailable gracefully")
    void testGetAssurancesByPatient_UserServiceUnavailable() {
        // Arrange
        List<Assurance> assurances = new ArrayList<>();
        assurances.add(testAssurance);
        when(assuranceRepository.findByPatientId(1L)).thenReturn(assurances);
        when(userServiceClient.getUserById(1L)).thenThrow(new RuntimeException("Service unavailable"));

        // Act
        List<AssuranceResponseDto> responses = assuranceService.getAssurancesByPatient(1L);

        // Assert
        assertEquals(1, responses.size());
        assertNotNull(responses.get(0).getId());
        verify(assuranceRepository, times(1)).findByPatientId(1L);
    }

    @Test
    @DisplayName("Should retrieve all assurances")
    void testGetAllAssurances_Success() {
        // Arrange
        List<Assurance> assurances = new ArrayList<>();
        assurances.add(testAssurance);
        when(assuranceRepository.findAll()).thenReturn(assurances);
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        // Act
        List<AssuranceResponseDto> responses = assuranceService.getAllAssurances();

        // Assert
        assertEquals(1, responses.size());
        assertEquals("Health Insurance Corp", responses.get(0).getProviderName());
        verify(assuranceRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no assurances exist")
    void testGetAllAssurances_Empty() {
        // Arrange
        List<Assurance> assurances = new ArrayList<>();
        when(assuranceRepository.findAll()).thenReturn(assurances);

        // Act
        List<AssuranceResponseDto> responses = assuranceService.getAllAssurances();

        // Assert
        assertEquals(0, responses.size());
        verify(assuranceRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should retrieve assurance by ID")
    void testGetAssuranceById_Success() {
        // Arrange
        when(assuranceRepository.findById(1L)).thenReturn(Optional.of(testAssurance));
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        // Act
        AssuranceResponseDto response = assuranceService.getAssuranceById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Health Insurance Corp", response.getProviderName());
        verify(assuranceRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when assurance not found by ID")
    void testGetAssuranceById_NotFound() {
        // Arrange
        when(assuranceRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> assuranceService.getAssuranceById(1L));
        verify(assuranceRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should retrieve assurances by list of IDs")
    void testGetAssurancesByIds_Success() {
        // Arrange
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        List<Assurance> assurances = new ArrayList<>();
        assurances.add(testAssurance);
        when(assuranceRepository.findAllById(ids)).thenReturn(assurances);
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        // Act
        List<AssuranceResponseDto> responses = assuranceService.getAssurancesByIds(ids);

        // Assert
        assertEquals(1, responses.size());
        assertEquals("Health Insurance Corp", responses.get(0).getProviderName());
        verify(assuranceRepository, times(1)).findAllById(ids);
    }

    @Test
    @DisplayName("Should return empty list when no assurances found for given IDs")
    void testGetAssurancesByIds_Empty() {
        // Arrange
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        List<Assurance> assurances = new ArrayList<>();
        when(assuranceRepository.findAllById(ids)).thenReturn(assurances);

        // Act
        List<AssuranceResponseDto> responses = assuranceService.getAssurancesByIds(ids);

        // Assert
        assertEquals(0, responses.size());
        verify(assuranceRepository, times(1)).findAllById(ids);
    }

    @Test
    @DisplayName("Should update assurance status successfully")
    void testUpdateAssuranceStatus_Success() {
        // Arrange
        Assurance updated = new Assurance();
        updated.setId(1L);
        updated.setPatientId(1L);
        updated.setProviderName("Health Insurance Corp");
        updated.setPolicyNumber("POL-12345-67890");
        updated.setCoverageDetails("Comprehensive coverage for Alzheimer's disease");
        updated.setIllness("Alzheimer's Disease");
        updated.setPostalCode("75001");
        updated.setMobilePhone("+33612345678");
        updated.setStatus(AssuranceStatus.APPROVED);
        updated.setCreatedAt(LocalDateTime.now());
        updated.setUpdatedAt(LocalDateTime.now());

        when(assuranceRepository.findById(1L)).thenReturn(Optional.of(testAssurance));
        when(assuranceRepository.save(any(Assurance.class))).thenReturn(updated);
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        // Act
        AssuranceResponseDto response = assuranceService.updateAssuranceStatus(1L, AssuranceStatus.APPROVED);

        // Assert
        assertNotNull(response);
        assertEquals(AssuranceStatus.APPROVED, response.getStatus());
        verify(assuranceRepository, times(1)).findById(1L);
        verify(assuranceRepository, times(1)).save(any(Assurance.class));
    }

    @Test
    @DisplayName("Should throw exception when updating status of non-existent assurance")
    void testUpdateAssuranceStatus_NotFound() {
        // Arrange
        when(assuranceRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, 
                () -> assuranceService.updateAssuranceStatus(1L, AssuranceStatus.APPROVED));
        verify(assuranceRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should update assurance successfully")
    void testUpdateAssurance_Success() {
        // Arrange
        AssuranceRequestDto updateRequest = new AssuranceRequestDto();
        updateRequest.setPatientId(1L);
        updateRequest.setProviderName("New Insurance Provider");
        updateRequest.setPolicyNumber("POL-99999-99999");
        updateRequest.setCoverageDetails("Updated coverage details");
        updateRequest.setIllness("Updated Illness");
        updateRequest.setPostalCode("75002");
        updateRequest.setMobilePhone("+33612345679");

        Assurance updated = new Assurance();
        updated.setId(1L);
        updated.setPatientId(1L);
        updated.setProviderName("New Insurance Provider");
        updated.setPolicyNumber("POL-99999-99999");
        updated.setCoverageDetails("Updated coverage details");
        updated.setIllness("Updated Illness");
        updated.setPostalCode("75002");
        updated.setMobilePhone("+33612345679");
        updated.setStatus(AssuranceStatus.PENDING);
        updated.setCreatedAt(LocalDateTime.now());
        updated.setUpdatedAt(LocalDateTime.now());

        when(assuranceRepository.findById(1L)).thenReturn(Optional.of(testAssurance));
        when(assuranceRepository.save(any(Assurance.class))).thenReturn(updated);
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        // Act
        AssuranceResponseDto response = assuranceService.updateAssurance(1L, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals("New Insurance Provider", response.getProviderName());
        assertEquals("POL-99999-99999", response.getPolicyNumber());
        verify(assuranceRepository, times(1)).findById(1L);
        verify(assuranceRepository, times(1)).save(any(Assurance.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent assurance")
    void testUpdateAssurance_NotFound() {
        // Arrange
        when(assuranceRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, 
                () -> assuranceService.updateAssurance(1L, testRequest));
        verify(assuranceRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should delete assurance successfully")
    void testDeleteAssurance_Success() {
        // Arrange
        when(assuranceRepository.findById(1L)).thenReturn(Optional.of(testAssurance));
        doNothing().when(assuranceRepository).delete(testAssurance);

        // Act
        assuranceService.deleteAssurance(1L);

        // Assert
        verify(assuranceRepository, times(1)).findById(1L);
        verify(assuranceRepository, times(1)).delete(testAssurance);
    }

    @Test
    @DisplayName("Should handle assurance status transitions")
    void testStatusTransitions() {
        // Arrange
        Assurance pending = new Assurance();
        pending.setId(1L);
        pending.setPatientId(1L);
        pending.setProviderName("Insurance Corp");
        pending.setPolicyNumber("POL-123");
        pending.setIllness("Illness");
        pending.setPostalCode("75001");
        pending.setMobilePhone("+33612345678");
        pending.setStatus(AssuranceStatus.PENDING);

        Assurance approved = new Assurance();
        approved.setId(1L);
        approved.setPatientId(1L);
        approved.setProviderName("Insurance Corp");
        approved.setPolicyNumber("POL-123");
        approved.setIllness("Illness");
        approved.setPostalCode("75001");
        approved.setMobilePhone("+33612345678");
        approved.setStatus(AssuranceStatus.APPROVED);

        when(assuranceRepository.findById(1L))
                .thenReturn(Optional.of(pending))
                .thenReturn(Optional.of(approved));
        when(assuranceRepository.save(any(Assurance.class)))
                .thenReturn(approved);
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        // Act
        AssuranceResponseDto response = assuranceService.updateAssuranceStatus(1L, AssuranceStatus.APPROVED);

        // Assert
        assertEquals(AssuranceStatus.APPROVED, response.getStatus());
    }
}
