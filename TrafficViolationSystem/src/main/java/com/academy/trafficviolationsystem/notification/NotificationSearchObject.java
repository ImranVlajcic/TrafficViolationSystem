package com.academy.trafficviolationsystem.notification;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Search/filter parameters for GET /api/notifications.
 *
 * All fields are optional. Null fields are ignored in NotificationService.additionalFilter().
 *
 * Example requests:
 *   GET /api/notifications?status=FAILED
 *   GET /api/notifications?userId=<uuid>&type=EMAIL
 *   GET /api/notifications?relatedEntityType=FINE&relatedEntityId=<uuid>
 */
@Getter
@Setter
public class NotificationSearchObject extends BaseSearchObject<UUID> {

    private NotificationStatus status;

    private NotificationType type;

    private UUID userId;

    private UUID relatedEntityId;

    private String relatedEntityType;

    /** created date range — start (inclusive). */
    private LocalDate fromDate;

    /** created date range — end (inclusive). */
    private LocalDate toDate;
}
