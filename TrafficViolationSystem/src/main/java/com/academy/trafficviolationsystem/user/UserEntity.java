package com.academy.trafficviolationsystem.user;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * A system user — officer, admin, or citizen portal account.
 *
 * Extends UUIDBaseEntity, so it inherits:
 *   id (UUID), created, updated, createdBy, updatedBy, deletedAt (soft-delete)
 *
 * Soft-delete is used instead of hard-delete so that audit logs and
 * violation records that reference a user ID remain valid after the user
 * is deactivated. Call repository.delete(entity) to soft-delete — the
 * @PreRemove hook in AbstractEntity sets deletedAt automatically.
 *
 * Password storage:
 *   Never store raw passwords. UserService hashes with BCrypt (strength 12)
 *   via the PasswordEncoder bean from SecurityConfig before saving.
 *
 * Account locking:
 *   After MAX_FAILED_LOGINS consecutive failures, lockedUntil is set to
 *   now + LOCK_DURATION_MINUTES. AuthService checks this before authenticating.
 */
@Getter
@Setter
@Entity
@SQLRestriction("deleted IS NULL")
@SQLDelete(sql = "UPDATE users SET deleted = now() WHERE id = ?")
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_user_email",    columnNames = "email"),
        @UniqueConstraint(name = "uk_user_badge",    columnNames = "badge_number")
    }
)
public class UserEntity extends UUIDBaseEntity {

    // ── identity ──────────────────────────────────────────────────────────

    @Column(name = "username", nullable = false, length = 60)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    @JsonIgnore
    private String passwordHash;

    // ── personal info ─────────────────────────────────────────────────────

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    // ── role & access ─────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    /**
     * Officer badge number. Null for CITIZEN and ADMIN accounts.
     * Stored and displayed on fine PDFs and violation records.
     */
    @Column(name = "badge_number", length = 30)
    private String badgeNumber;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // ── login tracking ────────────────────────────────────────────────────

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Counter reset to 0 on every successful login.
     * Incremented by AuthService on every failed attempt.
     */
    @Column(name = "failed_logins", nullable = false)
    private int failedLogins = 0;

    /**
     * Non-null while the account is temporarily locked.
     * AuthService checks: if lockedUntil is non-null and in the future → reject login.
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
}
