package com.academy.trafficviolationsystem.camera;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request body for POST /api/cameras.
 * ADMIN only — registering a new physical device.
 */
@Getter
@Setter
public class CameraCreateRequest {

    @NotBlank(message = "Serial number is required")
    @Size(max = 60)
    private String serialNumber;

    @NotBlank(message = "Name is required")
    @Size(max = 120)
    private String name;

    @NotNull(message = "Camera type is required")
    private CameraType cameraType;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0",  message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0",   message = "Latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0",  message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Min(value = 0, message = "Direction must be between 0 and 359")
    @Max(value = 359)
    private Integer directionDegrees;

    private String locationDescription;

    @Min(value = 1, message = "Speed limit must be positive")
    private Integer speedLimitKmh;

    @NotBlank(message = "MQTT topic is required")
    @Size(max = 200)
    private String mqttTopic;

    @PastOrPresent
    private LocalDate installDate;

    @Size(max = 40)
    private String firmwareVersion;
}
