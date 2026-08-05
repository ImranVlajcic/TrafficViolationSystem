package com.academy.trafficviolationsystem.notification;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of NotificationEntity.
 * Returned by all notification endpoints.
 */
@Getter
@Setter
public class NotificationDto {

    private UUID id;
    private NotificationType type;
    private String subject;
    private String body;
    private String recipient;
    private NotificationStatus status;
    private LocalDateTime sentAt;
    private String failureReason;
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private UUID relatedEntityId;
    private String relatedEntityType;
    private UUID userId;
    private Instant created;
}
