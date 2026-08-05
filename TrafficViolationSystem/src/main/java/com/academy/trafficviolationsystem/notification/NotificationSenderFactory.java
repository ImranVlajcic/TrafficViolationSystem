package com.academy.trafficviolationsystem.notification;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory Method — hands back the correct NotificationSender for a given
 * NotificationType without the caller (NotificationService) needing to know
 * about EmailNotificationSender / SmsNotificationSender / ... directly.
 *
 * Spring autowires every NotificationSender bean into the constructor list;
 * the factory indexes them by getType() once at startup. Adding a new
 * channel is then just: implement NotificationSender, annotate it
 * @Component, add a NotificationType enum value — this class needs no
 * changes.
 */
@Component
public class NotificationSenderFactory {

    private final Map<NotificationType, NotificationSender> senders = new EnumMap<>(NotificationType.class);

    public NotificationSenderFactory(List<NotificationSender> availableSenders) {
        for (NotificationSender sender : availableSenders) {
            senders.put(sender.getType(), sender);
        }
    }

    /**
     * Returns the sender registered for the given type.
     *
     * @throws IllegalStateException if no sender is registered for the type —
     *         indicates a NotificationType was added without a matching
     *         NotificationSender bean.
     */
    public NotificationSender getSender(NotificationType type) {
        NotificationSender sender = senders.get(type);
        if (sender == null) {
            throw new IllegalStateException("No NotificationSender registered for type: " + type);
        }
        return sender;
    }
}