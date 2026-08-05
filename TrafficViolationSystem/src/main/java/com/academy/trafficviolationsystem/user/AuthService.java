package com.academy.trafficviolationsystem.user;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.auth.*;
import com.academy.trafficviolationsystem.core.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final int MAX_FAILED_LOGINS     = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final AuthenticationManager  authenticationManager;
    private final JwtTokenProvider       jwtTokenProvider;
    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.expiration-ms:3600000}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository) {
        this.authenticationManager  = authenticationManager;
        this.jwtTokenProvider       = jwtTokenProvider;
        this.userRepository         = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // ── login ─────────────────────────────────────────────────────────────


    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AccountLockedException("Account is locked until " + user.getLockedUntil());
        }

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (BadCredentialsException e) {
            handleFailedLogin(user);
            throw new UnauthorizedException("Invalid credentials");
        } catch (LockedException e) {
            throw new AccountLockedException();
        } catch (DisabledException e) {
            throw new AccountDisabledException();
        }

        userRepository.recordSuccessfulLogin(user.getId(), LocalDateTime.now());

        return issueTokenPair(user);
    }

    // ── refresh ───────────────────────────────────────────────────────────

    public LoginResponse refresh(String rawRefreshToken) {
        RefreshTokenEntity stored = refreshTokenRepository.findByToken(hashToken(rawRefreshToken))
                .orElseThrow(() -> new TokenInvalidException("Refresh token not found"));

        if (stored.isRevoked()) {
            throw new TokenInvalidException();
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException();
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        UserEntity user = stored.getUser();
        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        return issueTokenPair(user);
    }

    // ── logout ────────────────────────────────────────────────────────────

    @AuditAction(value = "LOGOUT", entityClass = UserEntity.class)
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private LoginResponse issueTokenPair(UserEntity user) {
        String accessToken = jwtTokenProvider.generateToken(
                user.getId(), user.getUsername(), user.getRole().name());

        String rawToken = UUID.randomUUID().toString();

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setToken(hashToken(rawToken));   // store hash only
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.of(accessToken, rawToken, accessTokenExpirationMs, user); // raw goes to client only
    }

    private void handleFailedLogin(UserEntity user) {
        int newCount = user.getFailedLogins() + 1;
        userRepository.incrementFailedLogins(user.getId());

        if (newCount >= MAX_FAILED_LOGINS) {
            userRepository.lockAccount(user.getId(),
                    LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
        }
    }

    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash); // or Base64
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 is always available, but keep this honest
        }
    }
}