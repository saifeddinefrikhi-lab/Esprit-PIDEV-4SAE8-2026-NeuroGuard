package com.esprit.microservice.prescriptionservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SmsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${neuroguard.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${neuroguard.sms.base-url:https://api.twilio.com/2010-04-01/Accounts}")
    private String smsBaseUrl;

    @Value("${neuroguard.sms.account-sid:}")
    private String accountSid;

    @Value("${neuroguard.sms.auth-token:}")
    private String authToken;

    @Value("${neuroguard.sms.from-number:}")
    private String fromNumber;

    @Value("${neuroguard.sms.default-country-prefix:+216}")
    private String defaultCountryPrefix;

    public void sendSms(String toPhoneNumber, String message) {
        if (!smsEnabled) {
            log.info("[SMS] SMS disabled (neuroguard.sms.enabled=false). Skipping.");
            return;
        }

        String to = normalizePhoneNumber(toPhoneNumber);
        if (to == null || to.isBlank()) {
            log.warn("[SMS] Missing patient phone number. Skipping SMS.");
            return;
        }

        if (isBlank(accountSid) || isBlank(authToken) || isBlank(fromNumber)) {
            log.warn("[SMS] Missing SMS provider configuration (sid/token/from). Skipping SMS.");
            return;
        }

        try {
            log.info("[SMS] Sending SMS to {}", to);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(accountSid, authToken);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("To", to);
            body.add("From", fromNumber);
            body.add("Body", message);

            String url = smsBaseUrl + "/" + accountSid + "/Messages.json";
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[SMS] SMS sent successfully to {}", to);
            } else {
                log.warn("[SMS] SMS provider responded with status {}", response.getStatusCode().value());
            }
        } catch (Exception e) {
            log.error("[SMS] Failed to send SMS to {}: {}", to, e.getMessage(), e);
        }
    }

    private String normalizePhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            return null;
        }

        String normalized = rawPhoneNumber.replaceAll("[\\s()-]", "");
        if (!normalized.startsWith("+")) {
            if (normalized.startsWith("00")) {
                normalized = "+" + normalized.substring(2);
            } else {
                if (normalized.startsWith("0")) {
                    normalized = normalized.substring(1);
                }
                normalized = defaultCountryPrefix + normalized;
            }
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
