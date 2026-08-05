package com.academy.trafficviolationsystem.core.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security's view of an authenticated user.
 *
 * This object is placed into the SecurityContext by JwtAuthFilter after
 * a valid token is validated. Controllers access it via @CurrentUser:
 *
 * We store only the fields that security decisions need — no DB calls
 * are made during request processing once the filter has set this up.
 *
 * Role convention:
 *   Spring Security requires authorities prefixed with "ROLE_".
 *   Roles stored in the DB / JWT are plain names (ADMIN, OFFICER, CITIZEN).
 *   The prefix is added here so @PreAuthorize("hasRole('ADMIN')") works.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    private final String role;          // plain role name, e.g. "OFFICER"
    private final boolean active;

    // Populated from token claims — no password needed after authentication.
    private static final String NO_PASSWORD = "";

    public UserPrincipal(UUID id, String username, String email, String role, boolean active) {
        this.id       = id;
        this.username = username;
        this.email    = email;
        this.role     = role;
        this.active   = active;
    }

    // ── UserDetails ───────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // "ROLE_" prefix is required by Spring Security's hasRole() checks.
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return NO_PASSWORD;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    // ── convenience ───────────────────────────────────────────────────────

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isOfficer() {
        return "OFFICER".equals(role);
    }

    public boolean isCitizen() {
        return "CITIZEN".equals(role);
    }
}
