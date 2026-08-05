package com.academy.trafficviolationsystem.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * Creates, signs, and validates JWT access tokens.
 *
 * The secret and expiry are read from application.properties:
 *
 *   app.jwt.secret=<at-least-64-char-random-string>
 *   app.jwt.expiration-ms=3600000     # 1 hour
 *
 * Token claims layout:
 *   sub  → user UUID (String form)
 *   role → UserRole enum name, e.g. "OFFICER"
 *   iat  → issued-at timestamp
 *   exp  → expiry timestamp
 *
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final Key signingKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {

        // HMAC-SHA512 requires at least 64 bytes. Keys.hmacShaKeyFor will throw
        // a WeakKeyException at startup if the configured secret is too short —
        // fail fast is intentional.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    // ── token creation ────────────────────────────────────────────────────

    /**
     * Builds a signed JWT for the given user.
     *
     * @param userId   the user's UUID primary key
     * @param username the user's login name (stored for convenience, not used for auth)
     * @param role     the user's role name, e.g. "ADMIN"
     * @return signed JWT string
     */
    public String generateToken(UUID userId, String username, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // ── token parsing ─────────────────────────────────────────────────────

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String getUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // ── token validation ──────────────────────────────────────────────────

    /**
     * Returns true only when the token is well-formed, properly signed,
     * and not expired. Logs the specific failure reason for debugging.
     *
     * Note: the JwtAuthFilter calls this before calling getUserId/getRole,
     * so there is no need to guard those methods independently.
     */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("JWT malformed: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("JWT unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT empty/null: {}", e.getMessage());
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }

    // ── internal ──────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey) // 2. setSigningKey() is now verifyWith()
                .build()                     // 3. Build the parser
                .parseSignedClaims(token)    // 4. parseClaimsJws() is now parseSignedClaims()
                .getPayload();               // 5. getBody() is now getPayload()
    }
}
