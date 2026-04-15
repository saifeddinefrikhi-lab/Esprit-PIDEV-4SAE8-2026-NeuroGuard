package com.neuroguard.pharmacy.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @InjectMocks
    private SmsService smsService;

    @Mock
    private RestTemplate restTemplate;

    /**
     * Test sendSms with SMS disabled does not throw exception
     */
    @Test
    void sendSms_whenDisabled_doesNotThrow() {
        // Arrange
        ReflectionTestUtils.setField(smsService, "smsEnabled", false);

        // Act & Assert
        assertDoesNotThrow(() -> smsService.sendSms("+21650000000", "Test message"));
    }

    /**
     * Test sendSms with null phone number does not throw exception
     */
    @Test
    void sendSms_withNullPhoneNumber_doesNotThrow() {
        // Arrange
        ReflectionTestUtils.setField(smsService, "smsEnabled", false);

        // Act & Assert
        assertDoesNotThrow(() -> smsService.sendSms(null, "Test message"));
    }

    /**
     * Test sendSms with empty phone number does not throw exception
     */
    @Test
    void sendSms_withEmptyPhoneNumber_doesNotThrow() {
        // Arrange
        ReflectionTestUtils.setField(smsService, "smsEnabled", false);

        // Act & Assert
        assertDoesNotThrow(() -> smsService.sendSms("", "Test message"));
    }

    /**
     * Test normalizePhoneNumber handles various formats
     */
    @Test
    void sendSms_normalizePhoneNumber_handlesVariousFormats() {
        // Arrange
        ReflectionTestUtils.setField(smsService, "smsEnabled", false);
        ReflectionTestUtils.setField(smsService, "defaultCountryPrefix", "+216");

        // Act & Assert - All should not throw
        assertDoesNotThrow(() -> smsService.sendSms("+21650000000", "Test"));
        assertDoesNotThrow(() -> smsService.sendSms("0050000000", "Test"));
        assertDoesNotThrow(() -> smsService.sendSms("50000000", "Test"));
        assertDoesNotThrow(() -> smsService.sendSms("(50) 000-0000", "Test"));
    }
}
