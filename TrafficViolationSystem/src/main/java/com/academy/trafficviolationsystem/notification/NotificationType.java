package com.academy.trafficviolationsystem.notification;

/**
 * The channel used to deliver a notification.
 * Stored as STRING on NotificationEntity and NotificationTemplateEntity.
 *
 * EMAIL and SMS are fully implemented.
 * IN_APP is reserved for future WebSocket push to the citizen portal.
 */
public enum NotificationType {

    /** HTML email via JavaMailSender / SMTP. */
    EMAIL,

    /** Short text message via SMS provider (stubbed for internship). */
    SMS,

    /**
     * In-app push notification for the React frontend.
     * Not yet implemented — reserved for future WebSocket integration.
     */
    IN_APP
}
