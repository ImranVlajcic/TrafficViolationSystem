package com.academy.trafficviolationsystem.user;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Persisted JWT refresh token.
 *
 * When a user logs in, both an access token (short-lived, ~1 hour) and a
 * refresh token (longer-lived, ~7 days) are issued. When the access token
 * expires, the client sends the refresh token to POST /api/auth/refresh
 * to get a new pair without re-entering credentials.
 *
 * Token rotation:
 *   On every refresh, the old token is revoked (revoked = true) and a new
 *   one is issued. This means a stolen refresh token can only be used once
 *   before the legitimate client rotates it and the old one becomes invalid.
 *
 * Logout:
 *   POST /api/auth/logout sets revoked = true on the current refresh token,
 *   immediately invalidating the session.
 */
@Getter
@Setter
@Entity
@Table(
    name = "refresh_tokens",
    uniqueConstraints = @UniqueConstraint(name = "uk_rt_token", columnNames = "token")
)
public class RefreshTokenEntity extends UUIDBaseEntity {

    /**
     * The actual token string stored hashed (SHA-256).
     * The raw token is returned to the client on login; only the hash is stored
     * so a DB breach does not expose usable tokens.
     */
    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Set to true on logout or token rotation.
     * A revoked token is never accepted by AuthService even if not yet expired.
     */
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    // ── request context (for security auditing) ───────────────────────────

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address", length = 45)  // 45 chars covers IPv6
    private String ipAddress;

    // ── relationship ──────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_rt_user")
    )
    private UserEntity user;
}
