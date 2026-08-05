package com.academy.trafficviolationsystem.vehicle;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Year;

/**
 * Request body for PUT /api/vehicles/{id}.
 *
 * All fields are optional — null means keep existing value.
 * licensePlate and vin cannot be changed via this endpoint —
 * plate changes require deregistration and re-registration.
 * Owner transfers use the dedicated POST /api/vehicles/{id}/transfer-ownership.
 * Stolen flag uses POST /api/vehicles/{id}/mark-stolen and /mark-found.
 */
@Getter
@Setter
public class VehicleUpdateRequest {

    @Size(max = 60)
    private String make;

    @Size(max = 80)
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be 1900 or later")
    private Integer year;

    @AssertTrue(message = "Year cannot be greater than next calendar year")
    private boolean isYearValid() {
        return year == null || year <= Year.now().getValue() + 1;
    }

    @Size(max = 40)
    private String color;

    private VehicleType vehicleType;

    @Min(value = 1, message = "Engine CC must be positive")
    @Max(value = 20000, message = "Engine CC must be less then 20000")
    private Integer engineCc;

    private FuelType fuelType;

    @PastOrPresent
    private LocalDate registrationDate;

    @FutureOrPresent
    private LocalDate registrationExpiry;

    @AssertTrue(message = "Registration expiry must be exactly one year after registration date")
    private boolean isRegistrationDatesValid() {

        if (registrationDate == null || registrationExpiry == null) {
            return true;
        }

        return registrationExpiry.equals(registrationDate.plusYears(1));
    }

    /** Set to false to deregister the vehicle. */
    private Boolean isActive;
}
