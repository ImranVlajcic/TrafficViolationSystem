package com.academy.trafficviolationsystem.notification;

import com.academy.trafficviolationsystem.core.entities.AutoIdBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin-editable message templates stored in the database.
 *
 * Extends AutoIdBaseEntity — templates are config data, integer PK is fine.
 *
 * Template keys (pre-seeded by Flyway V4 migration):
 *   FINE_ISSUED               — sent when a fine is issued after violation confirmation
 *   PAYMENT_SUCCESS           — sent when a fine is successfully paid
 *   APPEAL_APPROVED           — sent when an appeal is approved
 *   APPEAL_REJECTED           — sent when an appeal is rejected
 *   SUSPENSION_NOTICE         — sent when a driver's license is suspended
 *   LICENSE_EXPIRY_WARNING    — sent 30 days before license expiry
 *   ACCOUNT_LOCKED            — sent when too many failed login attempts lock an account
 *   VIOLATION_CONFIRMED       — sent when an automatic violation is confirmed by an officer
 *
 * Variable placeholders use Mustache-style {{variableName}} syntax.
 * Available variables depend on the template key:
 *   {{driverName}}     — driver's full name
 *   {{fineNumber}}     — fine reference (FIN-2025-000001)
 *   {{amount}}         — fine amount with currency (BAM 150.00)
 *   {{dueDate}}        — fine payment deadline
 *   {{violationRef}}   — violation reference (TRF-2025-000123)
 *   {{violationType}}  — violation type (SPEEDING)
 *   {{appealNumber}}   — appeal reference (APP-2025-000001)
 *   {{decision}}       — APPROVED or REJECTED
 *   {{reviewNotes}}    — officer's decision reasoning
 *   {{suspendedUntil}} — suspension end date
 *   {{penaltyPoints}}  — penalty points applied
 *   {{licenseNumber}}  — driver's license number
 *   {{expiresAt}}      — license expiry date
 *   {{transactionId}}  — payment transaction ID
 */
@Getter
@Setter
@Entity
@Table(
    name = "notification_templates",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_tmpl_key_type_lang",
            columnNames = {"template_key", "type", "language"}
        )
    },
    indexes = {
        @Index(name = "idx_tmpl_key", columnList = "template_key, is_active")
    }
)
public class NotificationTemplateEntity extends AutoIdBaseEntity {

    /**
     * Machine-readable key used to look up the template.
     * e.g. "FINE_ISSUED", "PAYMENT_SUCCESS"
     * Same key can have entries for different types (EMAIL, SMS) and languages.
     */
    @Column(name = "template_key", nullable = false, length = 60)
    private String templateKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    /**
     * Email subject line — may contain {{variables}}.
     * Null for SMS templates (SMS has no subject).
     */
    @Column(name = "subject")
    private String subject;

    /**
     * Full message body with {{variable}} placeholders.
     * For EMAIL: can contain HTML markup.
     * For SMS: plain text, keep under 160 characters where possible.
     */
    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    /**
     * BCP-47 language tag: "bs" (Bosnian), "en" (English), "hr" (Croatian).
     * NotificationService selects the template matching the user's preferred language
     * (stored on UserEntity in a future enhancement), falling back to "bs".
     */
    @Column(name = "language", nullable = false, length = 5)
    private String language = "bs";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
