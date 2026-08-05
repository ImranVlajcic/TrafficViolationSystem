package com.academy.trafficviolationsystem.driver;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import com.academy.trafficviolationsystem.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

/**
 * A licensed driver registered in the traffic system.
 *
 * Extends UUIDBaseEntity — inherits id (UUID), created, updated,
 * createdBy, updatedBy, deletedAt (soft-delete).
 *
 * Relationship to UserEntity:
 *   A driver CAN have a linked citizen UserEntity (for portal access),
 *   but does not have to. Officers register drivers manually; the driver
 *   can later create a citizen account and link it via the userId field.
 *
 * Penalty points:
 *   penaltyPoints accumulates across violations. When it reaches the
 *   threshold defined in SystemConfig (default 12), DriverService
 *   triggers a suspension and writes a LicenseSuspensionEntity row.
 *   LicensePointResetJob resets points to 0 on January 1st each year.
 *
 * Suspension:
 *   isSuspended is the fast runtime flag checked on every violation lookup.
 *   suspendedUntil is the calculated lift date. LicensePointResetJob
 *   also clears suspensions when their end date passes.
 */
@Getter
@Setter
@Entity
@SQLRestriction("deleted IS NULL")
@SQLDelete(sql = "UPDATE drivers SET deleted = now() WHERE id = ?")
@Table(
    name = "drivers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_driver_license",    columnNames = "license_number"),
        @UniqueConstraint(name = "uk_driver_national_id",columnNames = "national_id")
    },
    indexes = {
        @Index(name = "idx_driver_suspended", columnList = "is_suspended"),
        @Index(name = "idx_driver_last_name",  columnList = "last_name")
    }
)
public class DriverEntity extends UUIDBaseEntity {

    // ── identity ──────────────────────────────────────────────────────────

    @Column(name = "license_number", nullable = false, length = 30)
    private String licenseNumber;

    @Column(name = "national_id", nullable = false, length = 20)
    private String nationalId;

    // ── personal info ─────────────────────────────────────────────────────

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    // ── license details ───────────────────────────────────────────────────

    /**
     * License category/class: A, B, C, D, BE, CE…
     */
    @Column(name = "license_category", nullable = false, length = 20)
    private String licenseCategory;

    @Column(name = "license_issued_at", nullable = false)
    private LocalDate licenseIssuedAt;

    @Column(name = "license_expires_at", nullable = false)
    private LocalDate licenseExpiresAt;

    // ── penalty points & suspension ───────────────────────────────────────

    /**
     * Current accumulated demerit points.
     * Added by DriverService.applyPenaltyPoints() after a fine is issued.
     * Reset annually by LicensePointResetJob.
     */
    @Column(name = "penalty_points", nullable = false)
    private int penaltyPoints = 0;

    /**
     * Fast flag checked before every violation and fine operation.
     * Set to true by DriverService.suspend(), cleared by DriverService.liftSuspension().
     */
    @Column(name = "is_suspended", nullable = false)
    private boolean isSuspended = false;

    /**
     * Calculated end date of the current suspension period.
     * Null when isSuspended = false.
     */
    @Column(name = "suspended_until")
    private LocalDate suspendedUntil;

    // ── portal account link ───────────────────────────────────────────────

    /**
     * Optional link to a citizen UserEntity.
     * Null until the driver registers a portal account and links it.
     * Used by citizen-facing endpoints to scope data to the logged-in driver.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        unique = true,
        foreignKey = @ForeignKey(name = "fk_driver_user")
    )
    private UserEntity user;
}
