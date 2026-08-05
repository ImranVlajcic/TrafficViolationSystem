package com.academy.trafficviolationsystem.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Response body for POST /api/auth/login and POST /api/auth/refresh.
 *
 * The client must store accessToken in memory (not localStorage) and
 * refreshToken in an HttpOnly cookie or secure storage, then send
 * "Authorization: Bearer <accessToken>" on every subsequent request.
 *
 * accessToken  — short-lived JWT (default 1 hour). Sent on every request.
 * refreshToken — long-lived opaque token (default 7 days). Used only to
 *                call POST /api/auth/refresh when the access token expires.
 */
@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;       // always "Bearer"
    private long   expiresIn;       // access token lifetime in seconds

    // User summary — saves the client an extra GET /api/users/me call
    private UUID   userId;
    private String username;
    private String role;

    public static LoginResponse of(String accessToken, String refreshToken,
                                   long expiresInMs, UserEntity user) {
        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresInMs / 1000,
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }
}
