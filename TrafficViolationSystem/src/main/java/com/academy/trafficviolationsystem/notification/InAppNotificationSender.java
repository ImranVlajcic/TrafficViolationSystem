package com.academy.trafficviolationsystem.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Bridge "ConcreteImplementor" for the IN_APP channel.
 *
 * Stub until WebSocket push is implemented — behaviour is unchanged from
 * the inline switch case that previously lived in
 * NotificationService.dispatchRaw(). Replace the log line with a real push
 * (e.g. STOMP/WebSocket broadcast to the recipient's user channel) once
 * that infrastructure exists.
 */
@Component
public class InAppNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationSender.class);

    @Override
    public boolean send(String recipient, String subject, String body) {
        log.debug("IN_APP notification queued for {} — WebSocket not yet implemented", recipient);
        return true; // treat as sent for now, same as previous inline behaviour
    }

    @Override
    public NotificationType getType() {
        return NotificationType.IN_APP;
    }
}