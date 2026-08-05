package com.academy.trafficviolationsystem.vehicle;

/**
 * Classification of a registered vehicle.
 *
 * Used in:
 *  - VehicleEntity.vehicleType  (stored as STRING in DB)
 *  - VehicleSearchObject        (filter by type)
 *  - FineRuleEntity             (different fine amounts per type, e.g. trucks fined more)
 */
public enum VehicleType {

    /** Standard passenger car. */
    CAR,

    /** Motorcycle or moped. */
    MOTORCYCLE,

    /** Light commercial van. */
    VAN,

    /** Heavy goods truck. */
    TRUCK,

    /** Public or commercial bus. */
    BUS,

    /** Agricultural tractor. */
    TRACTOR,

    /** Any other motorised vehicle not covered above. */
    OTHER
}
