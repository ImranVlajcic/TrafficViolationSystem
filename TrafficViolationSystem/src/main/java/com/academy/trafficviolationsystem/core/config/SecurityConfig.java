package com.academy.trafficviolationsystem.core.config;

import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the Traffic Violation System.
 *
 * Key decisions:
 *  - Stateless JWT — no sessions, no cookies.
 *  - Method-level security enabled via @EnableMethodSecurity so individual
 *    service or controller methods can use @PreAuthorize("hasRole('ADMIN')").
 *
 * URL-level access rules (coarse-grained):
 *  Public  → /api/auth/**   (login, token refresh)
 *          → /v3/api-docs/** and /swagger-ui/** (Swagger UI)
 *  ADMIN   → /api/users/**  /api/cameras/**  /api/fine-rules/**  /api/jobs/**
 *  OFFICER → /api/violations/** (write), /api/fines/** (issue)
 *  Any authenticated user → everything else under /api/**
 *
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper  objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper  = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── disable defaults not needed for REST APIs ─────────────────
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            // ── stateless — no session, no cookie ─────────────────────────
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── URL-level access rules ─────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Public endpoints
                .requestMatchers("/","/api/auth/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                    .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()

                // Admin-only management endpoints
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/cameras/**").hasAnyRole("ADMIN", "OFFICER")
                .requestMatchers("/api/fine-rules/**").hasRole("ADMIN")
                .requestMatchers("/api/jobs/**").hasRole("ADMIN")
                .requestMatchers("/api/audit/**").hasRole("ADMIN")

                // Officer + Admin: violations and fines management
                .requestMatchers(HttpMethod.POST, "/api/violations/**").hasAnyRole("ADMIN", "OFFICER")
                .requestMatchers(HttpMethod.PUT,  "/api/violations/**").hasAnyRole("ADMIN", "OFFICER")
                .requestMatchers(HttpMethod.POST, "/api/fines/**").hasAnyRole("ADMIN", "OFFICER")

                // Analytics (read-only, any authenticated user)
                .requestMatchers(HttpMethod.GET, "/api/analytics/**").authenticated()

                // Everything else under /api requires authentication
                .requestMatchers("/api/**").authenticated()

                // Anything not matched is denied by default
                .anyRequest().denyAll()
            )

            // ── custom error responses (JSON, not Spring's default HTML) ──
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(
                                    response.getOutputStream(),
                                    ApiResponse.fail(ErrorCode.UNAUTHORIZED, "Authentication required")
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(
                                    response.getOutputStream(),
                                    ApiResponse.fail(ErrorCode.FORBIDDEN, "You do not have permission to perform this action")
                            );
                        })
                )

            // ── JWT filter runs before Spring's username/password filter ──
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Exposes the AuthenticationManager bean so AuthController can call
     * authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(...))
     * during login without needing to wire up the full auth provider manually.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
