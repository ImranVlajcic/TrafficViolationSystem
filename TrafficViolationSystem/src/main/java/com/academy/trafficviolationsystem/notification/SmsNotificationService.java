package com.academy.trafficviolationsystem.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stub SMS dispatcher for the internship project.
 *
 * In production replace this class body with a real SMS provider call —
 * Twilio, AWS SNS, or a local Bosnian telecom REST API. The interface
 * (send method signature) stays the same.
 *
 * Current implementation: logs the message to the application log.
 * Check the console during development to verify SMS notifications are triggered.
 *
 * Production replacement example (Twilio):
 * <pre>
 *   com.twilio.Twilio.init(accountSid, authToken);
 *   Message.creator(
 *       new PhoneNumber(phoneNumber),
 *       new PhoneNumber(twilioFromNumber),
 *       body
 *   ).create();
 * </pre>
 *
 * This service never throws — exceptions are caught and false is returned
 * so NotificationService can schedule a retry.
 */
@Service
public class SmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    /**
     * Sends an SMS to the given phone number.
     *
     * @param phoneNumber Recipient phone number in international format (+387XXXXXXXX).
     * @param body        Message text. Keep under 160 characters for a single SMS segment.
     * @return true if sent successfully (or logged for stub), false on error.
     */
    public boolean send(String phoneNumber, String body) {
        try {
            // ── STUB: log instead of calling a real provider ───────────────
            log.info("SMS ► {} : {}", phoneNumber, body);
            // ── Replace the line above with real SMS provider call in production ──
            return true;

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}
