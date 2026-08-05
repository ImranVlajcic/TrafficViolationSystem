package com.academy.trafficviolationsystem.notification;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import com.academy.trafficviolationsystem.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One notification dispatched (or attempted) by the system.
 *
 * Extends UUIDBaseEntity — inherits id (UUID), created, updated,
 * createdBy, updatedBy, deletedAt (soft-delete).
 *
 * Persisted BEFORE dispatching so failures can be retried.
 * Write-first pattern: the row exists regardless of whether delivery succeeds.
 *
 * Retry logic (managed by NotificationRetryJob):
 *   Attempt 1 fails → status = RETRYING, nextRetryAt = now + 5 min,  retryCount = 1
 *   Attempt 2 fails → status = RETRYING, nextRetryAt = now + 15 min, retryCount = 2
 *   Attempt 3 fails → status = RETRYING, nextRetryAt = now + 60 min, retryCount = 3
 *   Attempt 4 fails → status = FAILED (permanent),                   retryCount = 4
 *
 * relatedEntityId / relatedEntityType:
 *   Polymorphic soft-reference to the domain object this notification concerns.
 *   e.g. relatedEntityType = "FINE", relatedEntityId = fineId
 *   No JPA FK — avoids dependencies on all domain modules.
 */
@Getter
@Setter
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notif_user",      columnList = "user_id"),
        @Index(name = "idx_notif_status",    columnList = "status"),
        @Index(name = "idx_notif_retry",     columnList = "next_retry_at"),
        @Index(name = "idx_notif_entity",    columnList = "related_entity_id"),
        @Index(name = "idx_notif_created",   columnList = "created DESC")
    }
)
public class NotificationEntity extends UUIDBaseEntity {

    // ── channel & content ─────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    /** Email subject line. Null for SMS (subject has no meaning there). */
    @Column(name = "subject")
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * Email address or phone number of the recipient.
     * Stored redundantly so the notification can be retried even if the
     * UserEntity is later modified or deactivated.
     */
    @Column(name = "recipient", nullable = false)
    private String recipient;

    // ── status & retry ────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failure_reason")
    private String failureReason;

    /** Number of dispatch attempts made so far (including the initial attempt). */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    /**
     * When the next retry should be attempted.
     * Null when status is PENDING or SENT or FAILED.
     * Set by NotificationService on dispatch failure using exponential backoff.
     */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    // ── domain link (polymorphic, no JPA FK) ──────────────────────────────

    /**
     * UUID of the domain entity this notification is about.
     * e.g. the FineEntity.id or ViolationEntity.id.
     * Null for general system notifications (e.g. account lock notices).
     */
    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    /**
     * Type discriminator for the related entity.
     * Values: "VIOLATION", "FINE", "PAYMENT", "APPEAL", "SUSPENSION".
     */
    @Column(name = "related_entity_type", length = 30)
    private String relatedEntityType;

    // ── relationship ──────────────────────────────────────────────────────

    /**
     * Target system user (if the recipient has a portal account).
     * Null for external recipients who are not registered users.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        foreignKey = @ForeignKey(name = "fk_notif_user")
    )
    private UserEntity user;
}
