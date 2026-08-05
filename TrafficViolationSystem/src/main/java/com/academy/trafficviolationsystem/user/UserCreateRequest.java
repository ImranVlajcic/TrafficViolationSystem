package com.academy.trafficviolationsystem.user;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for POST /api/users (admin creates a new user account).
 *
 * The raw password is accepted here and hashed by UserService.beforeInsert()
 * before it ever touches the entity. It is never logged or stored in plain text.
 *
 * Validation errors are returned as HTTP 400 with a field-error map by
 * GlobalExceptionHandler — no extra handling needed in the controller.
 */
@Getter
@Setter
public class UserCreateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 60, message = "Username must be between 3 and 60 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "Username can only contain letters, numbers, underscores, and dots")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
            message = "Password must contain at least one uppercase letter, one digit, and one special character"
    )
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 80)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 80)
    private String lastName;

    @Pattern(
            regexp = "^$|^\\+?[1-9]\\d{6,14}$",
            message = "Phone number must be a valid international number (e.g. +14155552671)"
    )
    private String phoneNumber;

    @NotNull(message = "Role is required")
    private UserRole role;

    /** Required when role is OFFICER, optional otherwise. */

    private String badgeNumber;

    @AssertTrue(message = "Badge number is required for OFFICER role")
    private boolean isBadgeNumberValid() {
        if (role == UserRole.OFFICER) {
            return badgeNumber != null && !badgeNumber.isBlank();
        }
        return true;
    }
}


