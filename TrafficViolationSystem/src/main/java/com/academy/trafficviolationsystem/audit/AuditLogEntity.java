package com.academy.trafficviolationsystem.audit;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable record of every state-changing action in the system.
 *
 * Extends UUIDBaseEntity for the id, created, updated fields but in
 * practice this entity is NEVER updated or soft-deleted after creation.
 * It is an append-only audit trail.
 *
 * Written exclusively by AuditAspect via AOP interception — never saved
 * directly by domain services or controllers.
 *
 * action naming convention:
 *   CREATE_VIOLATION, UPDATE_FINE, CONFIRM_VIOLATION,
 *   CANCEL_FINE, APPROVE_APPEAL, SUSPEND_DRIVER,
 *   LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED …
 *
 * beforeSnapshot / afterSnapshot:
 *   JSON serialisation of the entity state before and after the operation.
 *   Populated by AuditAspect using ObjectMapper.writeValueAsString().
 *   Null for CREATE operations (no "before" state) and for actions
 *   where capturing state is not meaningful (e.g. bulk job operations).
 *
 * actorId / actorUsername:
 *   actorUsername is denormalized at write time so the audit log remains
 *   readable even after a user is deleted or renamed.
 *   actorId is null when the action is performed by a background job or
 *   the MQTT pipeline — actorUsername will be "SYSTEM" in those cases.
 */
@Getter
@Setter
@Entity
@Table(
        name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_entity",  columnList = "entity_type, entity_id"),
                @Index(name = "idx_audit_actor",   columnList = "actor_id"),
                @Index(name = "idx_audit_time",    columnList = "occurred_at DESC"),
                @Index(name = "idx_audit_action",  columnList = "action")
        }
)
public class AuditLogEntity extends UUIDBaseEntity {

    // ── what happened ─────────────────────────────────────────────────────

    /**
     * Action identifier — what operation was performed.
     * e.g. "CONFIRM_VIOLATION", "CANCEL_FINE", "APPROVE_APPEAL"
     */
    @Column(name = "action", nullable = false, length = 60)
    private String action;

    // ── what was affected ─────────────────────────────────────────────────

    /**
     * Simple class name of the affected JPA entity.
     * e.g. "ViolationEntity", "FineEntity", "DriverEntity"
     *
     * Null for actions with no single affected record — e.g. LOGIN_SUCCESS,
     * LOGIN_FAILED, ACCOUNT_LOCKED, or actions whose @AuditAction did not
     * specify entityClass().
     */
    @Column(name = "entity_type", length = 60)
    private String entityType;

    /**
     * Primary key of the affected record.
     *
     * Null for the same class of actions as entityType above — there is no
     * single record to point at.
     */
    @Column(name = "entity_id")
    private UUID entityId;

    // ── who did it ────────────────────────────────────────────────────────

    /**
     * UUID of the UserEntity who performed the action.
     * Null when performed by background jobs or the MQTT pipeline.
     */
    @Column(name = "actor_id")
    private UUID actorId;

    /**
     * Username denormalised at write time.
     * "SYSTEM" for background jobs, MQTT processors, and scheduled tasks.
     */
    @Column(name = "actor_username", nullable = false, length = 60)
    private String actorUsername;

    /** IP address of the originating HTTP request. Null for background operations. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // ── what changed ──────────────────────────────────────────────────────

    /** JSON snapshot of the entity state BEFORE the operation. Null for CREATE. */
    @Column(name = "before_snapshot", columnDefinition = "TEXT")
    private String beforeSnapshot;

    /** JSON snapshot of the entity state AFTER the operation. */
    @Column(name = "after_snapshot", columnDefinition = "TEXT")
    private String afterSnapshot;

    /** Human-readable one-line summary of what happened. */
    @Column(name = "description")
    private String description;

    /** Exact timestamp of the action — set by AuditAspect, not JPA auditing. */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}