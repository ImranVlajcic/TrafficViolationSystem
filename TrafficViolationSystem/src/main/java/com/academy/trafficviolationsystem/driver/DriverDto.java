package com.academy.trafficviolationsystem.driver;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only projection of DriverEntity returned by all driver endpoints.
 *
 * licenseExpiryStatus and isLicenseExpired are computed fields
 * populated by DriverMapper using @AfterMapping — they are never
 * stored in the database.
 */
@Getter
@Setter
public class DriverDto {

    private UUID id;

    // identity
    private String licenseNumber;
    private String nationalId;

    // personal
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String email;
    private String phoneNumber;
    private String address;

    // license
    private String licenseCategory;
    private LocalDate licenseIssuedAt;
    private LocalDate licenseExpiresAt;

    /**
     * Computed in DriverMapper — true if licenseExpiresAt is before today.
     * Lets the frontend show a warning badge without extra logic.
     */
    private boolean licenseExpired;

    // penalty & suspension
    private int penaltyPoints;
    private boolean isSuspended;
    private LocalDate suspendedUntil;

    // portal link
    private UUID userId;

    // audit
    private String createdBy;
}
