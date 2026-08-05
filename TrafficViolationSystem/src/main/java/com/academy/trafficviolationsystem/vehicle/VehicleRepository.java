package com.academy.trafficviolationsystem.vehicle;

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
public interface VehicleRepository extends JpaRepository<VehicleEntity, UUID> {

    // ── lookups ───────────────────────────────────────────────────────────

    /**
     * Primary lookup key for camera/radar detection — license plate.
     * Called by CameraEventProcessorService on every MQTT event.
     */
    Optional<VehicleEntity> findByLicensePlateIgnoreCase(String licensePlate);

    Optional<VehicleEntity> findByVin(String vin);

    boolean existsByLicensePlateIgnoreCase(String licensePlate);

    boolean existsByVin(String vin);

    /** All active vehicles for a given owner. */
    List<VehicleEntity> findByOwnerIdAndIsActiveTrue(UUID ownerId);

    /** All vehicles (active or not) for a given owner — for full history view. */
    List<VehicleEntity> findByOwnerIdOrderByCreatedDesc(UUID ownerId);

    // ── stolen flag ───────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE VehicleEntity v SET v.isStolen = true  WHERE v.id = :id")
    void markStolen(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE VehicleEntity v SET v.isStolen = false WHERE v.id = :id")
    void markFound(@Param("id") UUID id);

    /** All currently stolen vehicles — for officer dashboard. */
    List<VehicleEntity> findByIsStolenTrueAndIsActiveTrue();

    // ── job / reporting queries ───────────────────────────────────────────

    /**
     * Vehicles whose registration expires within the specified window.
     * Used by a scheduled job to send renewal reminders.
     */
    @Query("""
        SELECT v FROM VehicleEntity v
        WHERE v.registrationExpiry BETWEEN :today AND :cutoff
          AND v.isActive = true
          AND v.deletedAt IS NULL
        """)
    List<VehicleEntity> findVehiclesWithExpiringRegistration(
            @Param("today")  LocalDate today,
            @Param("cutoff") LocalDate cutoff);
}
