package com.academy.trafficviolationsystem.vehicle;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

/**
 * Request body for POST /api/vehicles.
 * Used by officers and admins to register a new vehicle.
 *
 * licensePlate is normalised to uppercase in VehicleService.beforeInsert()
 * before the uniqueness check and entity save — clients can send either case.
 */
@Getter
@Setter
public class VehicleCreateRequest {

    @NotBlank(message = "License plate is required")
    @Size(max = 20, message = "License plate must not exceed 20 characters")
    @Pattern(
            regexp = "^(?i)[AEJKMOT]\\d{2}-[AEJKMOT]-\\d{3}$",
            message = "License plate must follow the BiH format (e.g., A12-K-345)"
    )
    private String licensePlate;

    @Size(min = 17, max = 17, message = "VIN must be exactly 17 characters")
    @Pattern(
            regexp = "^(?i)[A-HJ-NPR-Z0-9]{17}$",
            message = "Invalid VIN format (17 alphanumeric characters, excluding letters I, O, and Q)"
    )
    private String vin;

    @NotBlank(message = "Make is required")
    @Size(max = 60)
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 80)
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be 1900 or later")
    private Integer year;

    @AssertTrue(message = "Year cannot be greater than next calendar year")
    private boolean isYearValid() {
        return year == null || year <= Year.now().getValue() + 1;
    }

    @NotBlank(message = "Color is required")
    @Size(max = 40)
    private String color;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @Min(value = 1, message = "Engine CC must be positive")
    @Max(value = 20000, message = "Engine CC must be less then 20000")
    private Integer engineCc;

    private FuelType fuelType;

    @NotNull(message = "Registration date is required")
    @PastOrPresent
    private LocalDate registrationDate;

    @NotNull(message = "Registration expiry is required")
    @FutureOrPresent
    private LocalDate registrationExpiry;

    @AssertTrue(message = "Registration expiry must be exactly one year after registration date")
    private boolean isRegistrationDatesValid() {
        return registrationExpiry.equals(registrationDate.plusYears(1));
    }

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;
}
