package com.academy.trafficviolationsystem.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    // ── retry job queries ─────────────────────────────────────────────────

    /**
     * Finds RETRYING notifications whose retry window has elapsed.
     * Called by NotificationRetryJob every 15 minutes.
     */
    @Query("""
        SELECT n FROM NotificationEntity n
        WHERE n.status = 'RETRYING'
          AND n.nextRetryAt <= :now
          AND n.deletedAt IS NULL
        ORDER BY n.nextRetryAt ASC
        """)
    List<NotificationEntity> findDueForRetry(@Param("now") LocalDateTime now);

    // ── lookup ────────────────────────────────────────────────────────────

    /** All notifications related to a domain entity (fine, violation, etc.). */
    List<NotificationEntity> findByRelatedEntityIdOrderByCreatedDesc(UUID relatedEntityId);

    /** All notifications for a user — for citizen 'my notifications' endpoint. */
    List<NotificationEntity> findByUserIdOrderByCreatedDesc(UUID userId);

    // ── status updates ────────────────────────────────────────────────────

    @Modifying
    @Query("""
        UPDATE NotificationEntity n
        SET n.status = 'SENT', n.sentAt = :sentAt, n.failureReason = null
        WHERE n.id = :id
        """)
    void markSent(@Param("id") UUID id, @Param("sentAt") LocalDateTime sentAt);

    @Modifying
    @Query("""
    UPDATE NotificationEntity n
    SET n.status      = :status,
        n.failureReason = :reason,
        n.retryCount  = :retryCount,
        n.nextRetryAt = :nextRetryAt
    WHERE n.id = :id
    """)
    void markFailedWithRetry(@Param("id")           UUID id,
                             @Param("status")        NotificationStatus status,
                             @Param("reason")        String reason,
                             @Param("retryCount")    int retryCount,
                             @Param("nextRetryAt")   LocalDateTime nextRetryAt);
}
