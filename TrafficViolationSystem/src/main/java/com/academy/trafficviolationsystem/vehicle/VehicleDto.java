package com.academy.trafficviolationsystem.vehicle;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only projection of VehicleEntity returned by all vehicle endpoints.
 *
 * registrationExpired is a computed field set by VehicleMapper @AfterMapping —
 * it is never stored in the database.
 *
 * ownerFullName is a convenience field mapped from owner.firstName + owner.lastName
 * so the frontend does not need a second request to display the owner's name.
 */
@Getter
@Setter
public class VehicleDto {

    private UUID id;

    // identification
    private String licensePlate;
    private String vin;

    // vehicle details
    private String make;
    private String model;
    private int year;
    private String color;
    private VehicleType vehicleType;
    private Integer engineCc;
    private FuelType fuelType;

    // registration
    private LocalDate registrationDate;
    private LocalDate registrationExpiry;

    /**
     * Computed by VehicleMapper @AfterMapping.
     * True when registrationExpiry is before today.
     */
    private boolean registrationExpired;

    // status
    private boolean isStolen;
    private boolean isActive;

    // owner summary — avoids a second API call from the frontend
    private UUID ownerId;
    private String ownerFullName;
    private String ownerLicenseNumber;

    // audit
    private String createdBy;
}
