package com.academy.trafficviolationsystem.user;

import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridge between Spring Security's authentication machinery and UserEntity.
 *
 * Spring Security calls loadUserByUsername() during the login flow when
 * AuthenticationManager.authenticate() is invoked in AuthService.
 *
 * It is NOT called on every request — JwtAuthFilter rebuilds the principal
 * from the JWT token claims without hitting the database, which keeps
 * authenticated requests fast.
 *
 * It IS called exactly once: during login password verification.
 */
@Service
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Called by Spring Security with the value from LoginRequest.username.
     * Must return a UserDetails whose getPassword() Spring will compare
     * against the submitted password using PasswordEncoder.matches().
     *
     * @throws UsernameNotFoundException if no active user with this username exists.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with username: " + username));

        // Return our UserPrincipal with the hashed password so Spring Security
        // can verify it. The 'active' flag controls isAccountNonLocked().
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive()
        ) {
            // Override getPassword() to return the stored hash for login verification.
            // After login, JwtAuthFilter builds the principal from token claims with
            // no password, which is correct — credentials are not needed post-auth.
            @Override
            public String getPassword() {
                return user.getPasswordHash();
            }
        };
    }
}
