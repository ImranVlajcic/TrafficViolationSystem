package com.academy.trafficviolationsystem.camera;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request body for PUT /api/cameras/{id}.
 * All fields optional — null keeps existing value.
 * serialNumber and mqttTopic are immutable after registration.
 */
@Getter
@Setter
public class CameraUpdateRequest {

    @Size(max = 120)
    private String name;

    private CameraType cameraType;

    @Min(value = 0)
    @Max(value = 359)
    private Integer directionDegrees;

    private String locationDescription;

    @Min(value = 1)
    private Integer speedLimitKmh;

    @Size(max = 40)
    private String firmwareVersion;

    @PastOrPresent
    private LocalDate installDate;

    /** Set to false to decommission the camera. */
    private Boolean isActive;
}
