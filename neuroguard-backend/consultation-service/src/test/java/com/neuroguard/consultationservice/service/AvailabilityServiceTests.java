package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.AvailabilityRequest;
import com.neuroguard.consultationservice.dto.AvailabilityResponse;
import com.neuroguard.consultationservice.entity.DayOfWeek;
import com.neuroguard.consultationservice.entity.ProviderAvailability;
import com.neuroguard.consultationservice.exception.ResourceNotFoundException;
import com.neuroguard.consultationservice.exception.UnauthorizedException;
import com.neuroguard.consultationservice.repository.ProviderAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvailabilityService Tests")
class AvailabilityServiceTests {

    @Mock
    private ProviderAvailabilityRepository repository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private AvailabilityRequest validRequest;
    private ProviderAvailability savedAvailability;

    @BeforeEach
    void setUp() {
        validRequest = new AvailabilityRequest();
        validRequest.setDayOfWeek(DayOfWeek.MONDAY);
        validRequest.setStartTime(LocalTime.of(9, 0));
        validRequest.setEndTime(LocalTime.of(17, 0));

        savedAvailability = new ProviderAvailability();
        savedAvailability.setId(1L);
        savedAvailability.setProviderId(1L);
        savedAvailability.setDayOfWeek(DayOfWeek.MONDAY);
        savedAvailability.setStartTime(LocalTime.of(9, 0));
        savedAvailability.setEndTime(LocalTime.of(17, 0));
    }

    @Test
    @DisplayName("Should create availability successfully")
    void testCreateAvailability_Success() {
        // Arrange
        Long providerId = 1L;
        when(repository.save(any(ProviderAvailability.class))).thenReturn(savedAvailability);

        // Act
        AvailabilityResponse response = availabilityService.create(providerId, validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(providerId, response.getProviderId());
        assertEquals(DayOfWeek.MONDAY, response.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), response.getStartTime());
        assertEquals(LocalTime.of(17, 0), response.getEndTime());
        
        verify(repository, times(1)).save(any(ProviderAvailability.class));
    }

    @Test
    @DisplayName("Should reject availability with invalid times")
    void testCreateAvailability_InvalidTimes() {
        // Arrange
        AvailabilityRequest invalidRequest = new AvailabilityRequest();
        invalidRequest.setDayOfWeek(DayOfWeek.MONDAY);
        invalidRequest.setStartTime(LocalTime.of(17, 0));
        invalidRequest.setEndTime(LocalTime.of(9, 0)); // End before start

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> availabilityService.create(1L, invalidRequest),
            "Should reject availability with end time before start time");
    }

    @Test
    @DisplayName("Should update availability successfully")
    void testUpdateAvailability_Success() {
        // Arrange
        Long providerId = 1L;
        Long availabilityId = 1L;
        
        AvailabilityRequest updateRequest = new AvailabilityRequest();
        updateRequest.setDayOfWeek(DayOfWeek.TUESDAY);
        updateRequest.setStartTime(LocalTime.of(10, 0));
        updateRequest.setEndTime(LocalTime.of(18, 0));

        when(repository.findById(availabilityId)).thenReturn(Optional.of(savedAvailability));
        when(repository.save(any(ProviderAvailability.class))).thenReturn(savedAvailability);

        // Act
        AvailabilityResponse response = availabilityService.update(availabilityId, updateRequest, providerId);

        // Assert
        assertNotNull(response);
        verify(repository, times(1)).findById(availabilityId);
        verify(repository, times(1)).save(any(ProviderAvailability.class));
    }

    @Test
    @DisplayName("Should prevent unauthorized user from updating availability")
    void testUpdateAvailability_Unauthorized() {
        // Arrange
        Long differentProviderId = 2L;
        Long availabilityId = 1L;
        
        when(repository.findById(availabilityId)).thenReturn(Optional.of(savedAvailability));

        // Act & Assert
        assertThrows(UnauthorizedException.class,
            () -> availabilityService.update(availabilityId, validRequest, differentProviderId),
            "Should prevent different provider from updating availability");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent availability")
    void testUpdateAvailability_NotFound() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> availabilityService.update(999L, validRequest, 1L),
            "Should throw ResourceNotFoundException");
    }

    @Test
    @DisplayName("Should delete availability successfully")
    void testDeleteAvailability_Success() {
        // Arrange
        Long providerId = 1L;
        Long availabilityId = 1L;
        
        when(repository.findById(availabilityId)).thenReturn(Optional.of(savedAvailability));
        doNothing().when(repository).delete(any(ProviderAvailability.class));

        // Act
        availabilityService.delete(availabilityId, providerId);

        // Assert
        verify(repository, times(1)).findById(availabilityId);
        verify(repository, times(1)).delete(savedAvailability);
    }

    @Test
    @DisplayName("Should prevent unauthorized deletion")
    void testDeleteAvailability_Unauthorized() {
        // Arrange
        Long differentProviderId = 2L;
        when(repository.findById(1L)).thenReturn(Optional.of(savedAvailability));

        // Act & Assert
        assertThrows(UnauthorizedException.class,
            () -> availabilityService.delete(1L, differentProviderId),
            "Should prevent different provider from deleting availability");
    }

    @Test
    @DisplayName("Should retrieve availabilities by provider")
    void testGetByProvider_Success() {
        // Arrange
        Long providerId = 1L;
        List<ProviderAvailability> availabilities = new ArrayList<>();
        availabilities.add(savedAvailability);
        
        ProviderAvailability av2 = new ProviderAvailability();
        av2.setId(2L);
        av2.setProviderId(providerId);
        av2.setDayOfWeek(DayOfWeek.TUESDAY);
        av2.setStartTime(LocalTime.of(9, 0));
        av2.setEndTime(LocalTime.of(17, 0));
        availabilities.add(av2);

        when(repository.findByProviderId(providerId)).thenReturn(availabilities);

        // Act
        List<AvailabilityResponse> responses = availabilityService.getByProvider(providerId);

        // Assert
        assertEquals(2, responses.size());
        assertTrue(responses.stream().allMatch(r -> r.getProviderId().equals(providerId)));
        verify(repository, times(1)).findByProviderId(providerId);
    }

    @Test
    @DisplayName("Should return empty list when provider has no availabilities")
    void testGetByProvider_Empty() {
        // Arrange
        when(repository.findByProviderId(1L)).thenReturn(new ArrayList<>());

        // Act
        List<AvailabilityResponse> responses = availabilityService.getByProvider(1L);

        // Assert
        assertEquals(0, responses.size());
    }

    @Test
    @DisplayName("Should verify same time validation")
    void testCreateAvailability_SameStartEndTime() {
        // Arrange
        AvailabilityRequest sameTimeRequest = new AvailabilityRequest();
        sameTimeRequest.setDayOfWeek(DayOfWeek.MONDAY);
        sameTimeRequest.setStartTime(LocalTime.of(9, 0));
        sameTimeRequest.setEndTime(LocalTime.of(9, 0)); // Same time

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> availabilityService.create(1L, sameTimeRequest),
            "Should reject availability with same start and end time");
    }
}
