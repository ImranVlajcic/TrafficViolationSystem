package com.academy.trafficviolationsystem.core.security;

import com.academy.trafficviolationsystem.driver.DriverService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Authorization helper referenced from @PreAuthorize SpEL expressions as
 * "@securityHelper" — e.g.:
 *
 *   @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER') or @securityHelper.isOwnDriverRecord(#driverId, principal)")
 *
 * Bean name matches Spring's default (class name, first letter
 * lowercased) — no explicit @Component("securityHelper") needed for the
 * SpEL reference above to resolve.
 *
 * Already referenced by AppealController.getForDriver and
 * ViolationController.getForDriver's /driver/{driverId} routes.
 */
@Component
public class SecurityHelper {

    private final DriverService driverService;

    public SecurityHelper(DriverService driverService) {
        this.driverService = driverService;
    }

    /**
     * True if the given driverId is the driver record linked to
     * principal's user account. Used to let a citizen hit a
     * /driver/{driverId}-style endpoint for their own record without
     * needing ADMIN/OFFICER.
     *
     * Returns false (denies access) rather than throwing when the
     * principal has no linked driver record at all — an authorization
     * check should reject, not error out, in that case.
     */
    public boolean isOwnDriverRecord(UUID driverId, UserPrincipal principal) {
        if (principal == null || driverId == null) {
            return false;
        }
        UUID ownDriverId = resolveDriverIdOrNull(principal);
        return driverId.equals(ownDriverId);
    }

    /**
     * Resolves the driver id linked to principal's user account.
     * Used directly by /my-style endpoints (VehicleController,
     * ViolationController) that scope results to the current citizen
     * rather than checking a path-supplied id against it.
     *
     * Throws (via DriverService.getDriverIdForUser) if the principal has
     * no linked driver — appropriate here since /my endpoints have no
     * fallback ADMIN/OFFICER path the way /driver/{driverId} does.
     */
    public UUID resolveDriverId(UserPrincipal principal) {
        return driverService.getDriverIdForUser(principal.getId());
    }

    private UUID resolveDriverIdOrNull(UserPrincipal principal) {
        try {
            return driverService.getDriverIdForUser(principal.getId());
        } catch (Exception e) {
            return null;
        }
    }
}