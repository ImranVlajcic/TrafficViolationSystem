package com.academy.trafficviolationsystem.user;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of UserEntity returned by all user endpoints.
 *
 * passwordHash is intentionally excluded — never expose it in any response.
 * failedLogins and lockedUntil are excluded too; those are internal security
 * fields that the frontend does not need.
 */
@Getter
@Setter
public class UserDto {

    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserRole role;
    private String badgeNumber;
    private boolean isActive;
    private LocalDateTime lastLoginAt;

    // Inherited from BaseEntity — useful for admin displays
    private String createdBy;
    private String updatedBy;
}
