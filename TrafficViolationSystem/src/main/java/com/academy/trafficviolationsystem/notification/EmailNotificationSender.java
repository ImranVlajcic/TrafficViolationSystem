package com.academy.trafficviolationsystem.notification;

import org.springframework.stereotype.Component;

/**
 * Bridge "ConcreteImplementor" for the EMAIL channel.
 *
 * Thin adapter around EmailNotificationService, which owns the actual
 * JavaMailSender wiring. Keeping that wiring in a separate class (rather
 * than folding it in here) means EmailNotificationService can still be
 * unit-tested or reused independently of the Bridge/Factory machinery.
 */
@Component
public class EmailNotificationSender implements NotificationSender {

    private final EmailNotificationService emailNotificationService;

    public EmailNotificationSender(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @Override
    public boolean send(String recipient, String subject, String body) {
        return emailNotificationService.send(recipient, subject, body);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }
}