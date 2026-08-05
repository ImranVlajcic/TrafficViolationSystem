package com.academy.trafficviolationsystem.camera;

/**
 * Physical type of a traffic monitoring device.
 *
 * Determines which violation types the device can detect and which
 * MQTT payload fields to expect when processing CameraEventEntity rows.
 *
 * Used in:
 *  - CameraEntity.cameraType           (stored as STRING)
 *  - CameraEventProcessorService       (decides how to parse payload)
 *  - CameraSearchObject                (filter by type)
 */
public enum CameraType {

    /**
     * Fixed ANPR (Automatic Number Plate Recognition) camera.
     * Reads license plates and can detect red-light violations.
     * Payload contains: plate, imageUrl, timestamp, locationLat, locationLon.
     */
    ANPR,

    /**
     * Fixed speed radar with integrated plate reader.
     * Payload contains: plate, measuredSpeedKmh, speedLimitKmh,
     * imageUrl, timestamp, locationLat, locationLon.
     */
    SPEED_RADAR,

    /**
     * Dedicated red-light camera triggered by signal state + ANPR.
     * Payload contains: plate, imageUrl, videoUrl, timestamp, locationLat, locationLon.
     */
    RED_LIGHT,

    /**
     * Mobile unit — handheld or vehicle-mounted device used by officers.
     * Payload identical to SPEED_RADAR but location changes per deployment.
     */
    MOBILE_RADAR,

    /**
     * Overhead gantry camera covering multiple lanes.
     * Can detect wrong-lane, wrong-way, and ANPR simultaneously.
     */
    OVERHEAD
}
