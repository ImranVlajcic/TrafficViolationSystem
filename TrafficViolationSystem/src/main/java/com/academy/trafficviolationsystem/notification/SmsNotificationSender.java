package com.academy.trafficviolationsystem.notification;

import org.springframework.stereotype.Component;

/**
 * Bridge "ConcreteImplementor" for the SMS channel.
 *
 * SMS has no subject line, so the subject argument from the common
 * NotificationSender contract is simply dropped here.
 */
@Component
public class SmsNotificationSender implements NotificationSender {

    private final SmsNotificationService smsNotificationService;

    public SmsNotificationSender(SmsNotificationService smsNotificationService) {
        this.smsNotificationService = smsNotificationService;
    }

    @Override
    public boolean send(String recipient, String subject, String body) {
        return smsNotificationService.send(recipient, body);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.SMS;
    }
}