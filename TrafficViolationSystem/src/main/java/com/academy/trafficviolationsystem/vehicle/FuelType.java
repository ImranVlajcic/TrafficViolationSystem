package com.academy.trafficviolationsystem.vehicle;

/**
 * Fuel / propulsion type of a vehicle.
 * Used for reporting and potential future emission-based fine logic.
 */
public enum FuelType {

    GASOLINE,
    DIESEL,
    ELECTRIC,
    HYBRID,      // petrol + electric
    LPG,         // liquefied petroleum gas
    CNG,         // compressed natural gas
    HYDROGEN,
    OTHER
}
