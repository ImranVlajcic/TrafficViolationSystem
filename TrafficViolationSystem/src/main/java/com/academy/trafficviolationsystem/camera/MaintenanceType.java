package com.academy.trafficviolationsystem.camera;

/**
 * Category of maintenance work performed on a CameraEntity.
 * Used in CameraMaintenanceLogEntity.maintenanceType.
 */
public enum MaintenanceType {

    /** Routine physical check — cleaning, alignment, mounting inspection. */
    PHYSICAL_INSPECTION,

    /** Radar or speed measurement recalibration. */
    CALIBRATION,

    /** Over-the-air or physical firmware/software update. */
    FIRMWARE_UPDATE,

    /** Repair after detected fault or damage report. */
    FAULT_REPAIR,

    /** Complete hardware unit replacement. */
    HARDWARE_REPLACEMENT
}
