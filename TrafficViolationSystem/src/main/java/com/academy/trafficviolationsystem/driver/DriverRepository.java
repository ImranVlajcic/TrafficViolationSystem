package com.academy.trafficviolationsystem.driver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<DriverEntity, UUID> {

    // ── lookups ───────────────────────────────────────────────────────────

    Optional<DriverEntity> findByLicenseNumber(String licenseNumber);

    Optional<DriverEntity> findByNationalId(String nationalId);

    Optional<DriverEntity> findByUserId(UUID userId);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByNationalId(String nationalId);

    // ── suspension management ─────────────────────────────────────────────

    /**
     * Bulk-lifts suspensions whose end date has passed.
     * Called nightly by LicensePointResetJob.
     */
    @Modifying
    @Query("""
        UPDATE DriverEntity d
        SET d.isSuspended = false, d.suspendedUntil = null
        WHERE d.isSuspended = true
          AND d.suspendedUntil IS NOT NULL
          AND d.suspendedUntil <= :today
        """)
    int liftExpiredSuspensions(@Param("today") LocalDate today);

    /**
     * Annual reset: set all penalty points to 0.
     * Called by LicensePointResetJob on January 1st.
     */
    @Modifying
    @Query("UPDATE DriverEntity d SET d.penaltyPoints = 0 WHERE d.penaltyPoints > 0")
    int resetAllPenaltyPoints();

    // ── job queries ───────────────────────────────────────────────────────

    /** Returns all drivers currently suspended — used by job reporting. */
    List<DriverEntity> findByIsSuspendedTrue();

    /** Returns drivers whose license expires within the given number of days. */
    @Query("""
        SELECT d FROM DriverEntity d
        WHERE d.licenseExpiresAt BETWEEN :today AND :cutoff
          AND d.deletedAt IS NULL
        """)
    List<DriverEntity> findDriversWithExpiringLicenses(
            @Param("today")  LocalDate today,
            @Param("cutoff") LocalDate cutoff);
}
