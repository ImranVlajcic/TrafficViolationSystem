package com.academy.trafficviolationsystem.notification;

/**
 * Bridge "Implementor" — decouples NotificationService (the abstraction that
 * decides WHAT to send and WHEN) from the concrete channel used to send it.
 *
 * Each concrete sender wraps one delivery channel (email transport, SMS
 * gateway, in-app/WebSocket push, ...) behind the same three-argument
 * send(recipient, subject, body) contract, so NotificationService never
 * needs to know which channel-specific client it's talking to.
 *
 * Adding a new channel (push notifications, Viber, WhatsApp) means adding
 * one new implementation of this interface plus one NotificationType enum
 * value — NotificationService and NotificationSenderFactory require no
 * further changes beyond registering the new bean.
 */
public interface NotificationSender {

    /**
     * Sends the notification through this sender's channel.
     *
     * @param recipient email address, phone number, or user identifier depending on channel
     * @param subject   subject line; channels that don't support one (SMS, in-app) ignore it
     * @param body      message body
     * @return true if the message was accepted for delivery, false on failure
     */
    boolean send(String recipient, String subject, String body);

    /**
     * The NotificationType this sender handles. Used by NotificationSenderFactory
     * to build its lookup table.
     */
    NotificationType getType();
}