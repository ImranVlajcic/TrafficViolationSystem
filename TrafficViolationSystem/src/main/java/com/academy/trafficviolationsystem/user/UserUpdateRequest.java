package com.academy.trafficviolationsystem.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for PUT /api/users/{id}.
 *
 * All fields are optional (null = keep existing value).
 * UserService.beforeUpdate() applies only the non-null fields,
 * so partial updates work without the client resending everything.
 *
 * Password changes go through a dedicated endpoint
 * POST /api/users/{id}/change-password (not handled here) so they
 * can require the current password for confirmation.
 *
 * Role changes are admin-only and validated in UserService.beforeUpdate().
 */
@Getter
@Setter
public class UserUpdateRequest {

    @Email(message = "Email must be a valid email address")
    private String email;

    @Size(max = 80)
    private String firstName;

    @Size(max = 80)
    private String lastName;

    @Pattern(
            regexp = "^$|^\\+?[1-9]\\d{6,14}$",
            message = "Phone number must be a valid international number (e.g. +14155552671)"
    )
    private String phoneNumber;

    private UserRole role;

    private String badgeNumber;

    /** Set to false to soft-disable the account (blocks login). */
    private Boolean isActive;
}
