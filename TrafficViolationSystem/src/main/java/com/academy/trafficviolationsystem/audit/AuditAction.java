package com.academy.trafficviolationsystem.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for audit logging.
 *
 * Applied to service methods that perform state-changing operations.
 * AuditAspect intercepts methods annotated with @AuditAction and
 * writes an AuditLogEntity row capturing who did what and what changed.
 *
 * Usage:
 * <pre>
 *   {@literal @}AuditAction(value = "CONFIRM_VIOLATION", entityClass = ViolationEntity.class)
 *   public ViolationDto confirm(UUID id, ReviewViolationRequest request, UserPrincipal principal) { ... }
 *
 *   {@literal @}AuditAction(value = "CANCEL_FINE", entityClass = FineEntity.class)
 *   public FineDto cancel(UUID fineId, String reason, UserPrincipal principal) { ... }
 *
 *   {@literal @}AuditAction(value = "SUSPEND_DRIVER", entityClass = DriverEntity.class)
 *   public LicenseSuspensionDto suspend(UUID driverId, SuspendDriverRequest request, UserPrincipal principal) { ... }
 * </pre>
 *
 * The aspect extracts:
 *   - action     : the value() from this annotation
 *   - entityType : entityClass().getSimpleName(), or null if entityClass() is not set
 *   - entityId   : the first UUID-typed parameter (by convention the entity ID);
 *                  null if no UUID parameter is found — expected for actions
 *                  with no single affected record, e.g. LOGIN_SUCCESS
 *   - actorUsername : from SecurityContextHolder (falls back to "SYSTEM")
 *   - ipAddress  : from RequestContextHolder if in web context
 *   - snapshots  : before and after state serialised by ObjectMapper
 *
 * Keep action names in SCREAMING_SNAKE_CASE and consistent across the system.
 * Recommended action names:
 *   CREATE_{ENTITY}, UPDATE_{ENTITY}, DELETE_{ENTITY}
 *   CONFIRM_VIOLATION, DISMISS_VIOLATION, CONFIRM_VIOLATION
 *   ISSUE_FINE, CANCEL_FINE, MARK_FINE_PAID
 *   APPROVE_APPEAL, REJECT_APPEAL, WITHDRAW_APPEAL
 *   SUSPEND_DRIVER, LIFT_SUSPENSION
 *   PAY_FINE, TRANSFER_OWNERSHIP
 *   LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditAction {

    /**
     * The action name to record. Use SCREAMING_SNAKE_CASE.
     * e.g. "CONFIRM_VIOLATION", "CANCEL_FINE"
     */
    String value();

    /**
     * The JPA entity class this action affects.
     * Used to populate AuditLogEntity.entityType with the simple class name.
     */
    Class<?> entityClass() default Object.class;

    /**
     * Whether to capture before/after entity snapshots.
     * Set to false for high-volume read operations or bulk jobs where
     * snapshot capture would be too expensive.
     * Default: true.
     */
    boolean captureSnapshot() default true;
}