package com.academy.trafficviolationsystem.violation;

/**
 * Classifies what kind of traffic infraction occurred.
 *
 * Used in:
 *  - ViolationEntity.violationType        (stored as STRING in DB)
 *  - FineRuleEntity.violationType         (one rule row per type)
 *  - ViolationSearchObject.violationType  (filter)
 *  - ViolationLocationLogEntity           (denormalised for heatmap queries)
 *
 * Penalty points per type are defined in FineRuleEntity, not here.
 */
public enum ViolationType {

    /** Measured speed exceeds the posted limit at the detection point. */
    SPEEDING,

    /** Vehicle crossed a red traffic signal. */
    RED_LIGHT,

    /** Driver or passenger not wearing a seatbelt. */
    NO_SEATBELT,

    /** Driver operating a handheld device while moving. */
    PHONE_USE,

    /** Vehicle travelling in the wrong direction on a one-way road or lane. */
    WRONG_WAY,

    /** Illegal parking, stopping in a no-stop zone, or blocking access. */
    PARKING,

    /** Driving under the influence of alcohol or drugs. */
    DUI,

    /** Vehicle has no valid insurance policy. */
    NO_INSURANCE,

    /** Vehicle weight or cargo exceeds the legal limit. */
    OVERLOAD,

    /** Vehicle overtook on a solid line, in a tunnel, or at a crossing. */
    ILLEGAL_OVERTAKE,

    /** Vehicle drove in a lane reserved for buses, cycles, or emergency vehicles. */
    WRONG_LANE,

    /** Vehicle failed to yield at a pedestrian crossing. */
    PEDESTRIAN_CROSSING,

    /** Vehicle registration has expired. */
    EXPIRED_REGISTRATION,

    /** Any other infraction not covered by the categories above. */
    OTHER
}
