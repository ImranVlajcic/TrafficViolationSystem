package com.academy.trafficviolationsystem.user;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints — all public (no JWT required).
 * Mapped to /api/auth/** which is whitelisted in SecurityConfig.
 *
 *   POST /api/auth/login    → returns access + refresh token
 *   POST /api/auth/refresh  → rotates refresh token, returns new pair
 *   POST /api/auth/logout   → revokes all refresh tokens for current user
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username and password",
               description = "Returns a JWT access token and a refresh token. " +
                             "Pass the access token as 'Authorization: Bearer <token>' on all secured requests.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh an expired access token",
               description = "Provide a valid refresh token to receive a new access + refresh token pair. " +
                             "The old refresh token is immediately revoked (rotation).")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestParam String refreshToken) {
        LoginResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — revoke all active sessions for the current user")
    public ResponseEntity<ApiResponse<Void>> logout(@CurrentUser UserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }
}
