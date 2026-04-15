package com.neuroguard.assuranceservice.config;

import com.twilio.Twilio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@Slf4j
public class SmsConfig {

    private String accountSid;
    private String authToken;
    private String fromPhoneNumber;

    @PostConstruct
    public void initTwilio() {
        // Try System properties first (from .env via EnvConfig), then System environment
        this.accountSid = System.getProperty("TWILIO_ACCOUNT_SID") != null ? System.getProperty("TWILIO_ACCOUNT_SID") : System.getenv("TWILIO_ACCOUNT_SID");
        this.authToken = System.getProperty("TWILIO_AUTH_TOKEN") != null ? System.getProperty("TWILIO_AUTH_TOKEN") : System.getenv("TWILIO_AUTH_TOKEN");
        this.fromPhoneNumber = System.getProperty("TWILIO_PHONE_NUMBER") != null ? System.getProperty("TWILIO_PHONE_NUMBER") : System.getenv("TWILIO_PHONE_NUMBER");

        // Use default if not set
        if (this.fromPhoneNumber == null || this.fromPhoneNumber.isEmpty()) {
            this.fromPhoneNumber = "+1234567890";
        }

        log.info("Twilio SMS Configuration:");
        log.info("   Account SID: {}", this.accountSid != null && !this.accountSid.isEmpty() ? "***configured***" : "NOT SET");
        log.info("   Auth Token: {}", this.authToken != null && !this.authToken.isEmpty() ? "***configured***" : "NOT SET");
        log.info("   Phone Number: {}", this.fromPhoneNumber);

        if (accountSid != null && !accountSid.isEmpty() && authToken != null && !authToken.isEmpty()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized successfully");
        } else {
            log.warn("Twilio NOT configured - SMS notifications will be disabled");
        }
    }

    public String getAccountSid() {
        return accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getFromPhoneNumber() {
        return fromPhoneNumber;
    }

    public boolean isTwilioConfigured() {
        return accountSid != null && !accountSid.isEmpty() && authToken != null && !authToken.isEmpty();
    }
}
