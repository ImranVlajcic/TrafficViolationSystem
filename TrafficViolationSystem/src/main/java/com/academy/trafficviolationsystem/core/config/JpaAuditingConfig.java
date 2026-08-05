package com.academy.trafficviolationsystem.core.config;

import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA auditing configuration.
 *
 * The AuditorAware bean reads the current user from the SecurityContext.
 * For background jobs and MQTT events (no authenticated user), it returns
 * "SYSTEM" so audit fields are never null.
 *
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Provides the current actor's username for @CreatedBy / @LastModifiedBy fields.
     *
     * Returns:
     *   - The authenticated user's username when a request is in progress.
     *   - "SYSTEM" for background jobs, MQTT handlers, and unauthenticated contexts.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of("SYSTEM");
            }

            Object principal = auth.getPrincipal();

            if (principal instanceof UserPrincipal userPrincipal) {
                return Optional.of(userPrincipal.getUsername());
            }

            // Fallback for non-JWT auth (e.g. in tests using WithMockUser)
            return Optional.of(auth.getName());
        };
    }
}
