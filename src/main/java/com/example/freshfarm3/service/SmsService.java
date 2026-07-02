package com.example.freshfarm3.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    /**
     * Sends an SMS message asynchronously via Twilio REST API.
     * If Twilio credentials are not configured, logs a warning and skips.
     */
    @Async
    public void send(String toPhone, String message) {
        if (accountSid == null || accountSid.isBlank()) {
            log.warn("Twilio not configured. SMS skipped for: {}", toPhone);
            return;
        }
        try {
            // Twilio REST API call using Java SDK
            com.twilio.Twilio.init(accountSid, authToken);
            com.twilio.rest.api.v2010.account.Message.creator(
                    new com.twilio.type.PhoneNumber(toPhone),
                    new com.twilio.type.PhoneNumber(fromNumber),
                    message
            ).create();
            log.info("SMS sent to: {}", toPhone);
        } catch (Exception e) {
            // Non-critical — log and continue
            log.error("Failed to send SMS to {}: {}", toPhone, e.getMessage());
        }
    }
}