package com.academy.trafficviolationsystem.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Intercepts every incoming HTTP request and populates the SecurityContext
 * when a valid JWT is present in the Authorization header.
 *
 * Flow:
 *   1. Extract the Bearer token from the Authorization header.
 *   2. Validate it with JwtTokenProvider (signature + expiry).
 *   3. Build a UserPrincipal from the token claims (no DB lookup needed).
 *   4. Set the authentication on the SecurityContextHolder.
 *   5. Call the next filter in the chain regardless.
 *
 * If the token is absent or invalid the filter simply does nothing —
 * the request continues unauthenticated and Spring Security will reject
 * it at the method/URL level if authentication is required there.
 *
 * This filter runs once per request (OncePerRequestFilter) and is
 * stateless — no sessions are created or used.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && tokenProvider.validate(token)) {
            try {
                UUID   userId   = tokenProvider.getUserId(token);
                String username = tokenProvider.getUsername(token);
                String role     = tokenProvider.getRole(token);

                // Build the principal from token claims only — no DB round-trip.
                UserPrincipal principal = new UserPrincipal(userId, username, null, role, true);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,                       // credentials not needed post-auth
                                principal.getAuthorities()
                        );

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Place in SecurityContext so @PreAuthorize and @CurrentUser work.
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                // If anything goes wrong building the principal, log and continue
                // unauthenticated — never let a malformed token crash the request.
                log.warn("Could not set user authentication from token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    // ── helper ────────────────────────────────────────────────────────────

    /**
     * Extracts the raw JWT string from the "Authorization: Bearer <token>" header.
     * Returns null if the header is absent or not in Bearer format.
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
