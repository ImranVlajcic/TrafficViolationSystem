package com.academy.trafficviolationsystem.vehicle;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

/**
 * A registered vehicle in the traffic system.
 *
 * Extends UUIDBaseEntity — inherits id (UUID), created, updated,
 * createdBy, updatedBy, deletedAt (soft-delete).
 *
 * One driver (owner) can have many vehicles.
 * One vehicle is always linked to exactly one owner at a time.
 * When ownership changes, VehicleService writes a VehicleOwnershipHistoryEntity
 * row and updates the owner FK — the full transfer history is preserved.
 *
 * Stolen flag:
 *   When isStolen = true, MqttViolationListener and CameraEventProcessorService
 *   raise an elevated-priority alert on top of any normal violation, so officers
 *   are immediately notified even if no speed violation occurred.
 *
 * Registration expiry:
 *   OverdueFineCheckerJob (or a dedicated job) can query vehicles where
 *   registrationExpiry < today and notify owners to renew.
 */
@Getter
@Setter
@Entity
@SQLDelete(sql = "UPDATE vehicles SET deleted = now() WHERE id = ?")
@SQLRestriction("deleted IS NULL")
@Table(
    name = "vehicles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vehicle_plate", columnNames = "license_plate"),
        @UniqueConstraint(name = "uk_vehicle_vin",   columnNames = "vin")
    },
    indexes = {
        @Index(name = "idx_vehicle_owner",  columnList = "owner_id"),
        @Index(name = "idx_vehicle_stolen", columnList = "is_stolen"),
        @Index(name = "idx_vehicle_active", columnList = "is_active")
    }
)
public class VehicleEntity extends UUIDBaseEntity {

    // ── identification ────────────────────────────────────────────────────

    /**
     * License plate number — the primary lookup key used by cameras/radars.
     * Format varies by country; stored as-is, uppercase-normalised in VehicleService.
     */
    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    /**
     * Vehicle Identification Number — optional, unique if present.
     * Null for older vehicles that predate VIN standardisation.
     */
    @Column(name = "vin", length = 17)
    private String vin;

    // ── vehicle details ───────────────────────────────────────────────────

    @Column(name = "make", nullable = false, length = 60)
    private String make;

    @Column(name = "model", nullable = false, length = 80)
    private String model;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "color", nullable = false, length = 40)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    /** Engine displacement in cubic centimetres. Null for electric vehicles. */
    @Column(name = "engine_cc")
    private Integer engineCc;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", length = 20)
    private FuelType fuelType;

    // ── registration ──────────────────────────────────────────────────────

    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    @Column(name = "registration_expiry", nullable = false)
    private LocalDate registrationExpiry;

    // ── status flags ──────────────────────────────────────────────────────

    /**
     * Set to true when the vehicle is reported stolen.
     * VehicleService.markStolen() / markFound() toggle this flag and write
     * a notification. MqttViolationListener checks this on every camera event.
     */
    @Column(name = "is_stolen", nullable = false)
    private boolean isStolen = false;

    /**
     * Set to false when the vehicle is deregistered or scrapped.
     * Soft-delete from the active fleet without removing historical records.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // ── ownership ─────────────────────────────────────────────────────────

    /**
     * Current registered owner.
     * When ownership transfers, VehicleService updates this FK and writes
     * a VehicleOwnershipHistoryEntity row before saving.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "owner_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_vehicle_owner")
    )
    private DriverEntity owner;
}
