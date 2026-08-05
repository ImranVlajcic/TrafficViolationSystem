package com.academy.trafficviolationsystem.driver;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request body for POST /api/drivers.
 * Used by officers and admins to register a new driver.
 */
@Getter
@Setter
public class DriverCreateRequest {

    @NotBlank(message = "License number is required")
    @Size(max = 30, message = "License number must not exceed 30 characters")
    private String licenseNumber;

    @NotBlank(message = "National ID is required")
    @Size(max = 20, message = "National ID must not exceed 20 characters")
    private String nationalId;

    @NotBlank(message = "First name is required")
    @Size(max = 80)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 80)
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Email(message = "Email must be a valid email address")
    private String email;

    @Pattern(
            regexp = "^$|^\\+?[1-9]\\d{6,14}$",
            message = "Phone number must be a valid international number (e.g. +14155552671)"
    )
    private String phoneNumber;

    private String address;

    @NotBlank(message = "License category is required")
    @Size(max = 20)
    private String licenseCategory;

    @NotNull(message = "License issued date is required")
    @PastOrPresent
    private LocalDate licenseIssuedAt;

    @NotNull(message = "License expiry date is required")
    @FutureOrPresent
    private LocalDate licenseExpiresAt;
}
