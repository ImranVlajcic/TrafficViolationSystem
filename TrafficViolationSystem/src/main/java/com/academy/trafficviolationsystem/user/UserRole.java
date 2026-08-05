package com.academy.trafficviolationsystem.user;

/**
 * Roles available in the system.
 *
 * Used in:
 *  - UserEntity.role  (stored as STRING in DB)
 *  - JWT claim "role" (the plain name, e.g. "ADMIN")
 *  - SecurityConfig URL rules  (.hasRole("ADMIN"))
 *  - @PreAuthorize expressions (@PreAuthorize("hasRole('OFFICER')"))
 *
 * Spring Security requires the "ROLE_" prefix internally, but UserPrincipal
 * adds it automatically — you always store and compare the plain name here.
 */
public enum UserRole {

    /** Full system access. Can manage users, cameras, fine rules, view audit logs. */
    ADMIN,

    /** Traffic officer. Can record violations, issue fines, review appeals. */
    OFFICER,

    /** Registered citizen. Can view their own violations, pay fines, file appeals. */
    CITIZEN,

    /** Internal identity used by background jobs and MQTT processors. Never logs in. */
    SYSTEM
}
