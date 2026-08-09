package com.academy.trafficviolationsystem.audit;

import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spring AOP aspect that intercepts @AuditAction-annotated service methods
 * and writes AuditLogEntity rows automatically.
 *
 * This runs around every method marked with @AuditAction. The actual
 * business operation proceeds normally — the aspect is purely additive.
 *
 * CRITICAL: Writing the audit row is wrapped in its own try-catch. An audit
 * logging failure must NEVER break the actual business operation. If writing
 * the audit row fails, the error is logged but the method's result (or
 * exception) is still returned/thrown normally.
 *
 * How entity ID is extracted:
 *   The aspect looks at the method arguments for the first parameter that
 *   is a UUID, Integer, or Long. By convention all service methods that
 *   modify an entity have the entity ID as their first argument. Non-UUID
 *   PKs (e.g. CameraEntity's Integer id) are deterministically converted to
 *   a UUID for storage — see toStorableEntityId(). If no matching argument
 *   is found, entityId is recorded as null (expected for actions with no
 *   single affected record, e.g. LOGIN_SUCCESS).
 *
 * How snapshots are captured:
 *   Before snapshot: if the entity class is known (entityClass() set) and
 *   captureSnapshot=true, the aspect calls entityManager.find(entityClass,
 *   entityId) before the method runs and serialises the result to JSON.
 *   After snapshot: the return value of the method is serialised to JSON
 *   (the DTO returned by the service method acts as the "after" state).
 *   Skipped entirely when the method throws — there is no "after" state
 *   for a failed operation.
 *
 * Failed operations:
 *   proceed() is wrapped in try/catch. If the underlying method throws, the
 *   audit row is still written (marked FAILED in its description) and the
 *   original exception is rethrown afterwards, so callers see the same
 *   behavior as if this aspect weren't here.
 *
 * Thread safety:
 *   SecurityContextHolder and RequestContextHolder are thread-local —
 *   safe to read from any thread. ObjectMapper is thread-safe after construction.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper       objectMapper;
    private final EntityManager      entityManager;
    private final AuditWriter auditWriter;
    private final AuditSnapshotReader auditSnapshotReader;

    public AuditAspect(AuditLogRepository auditLogRepository,
                       ObjectMapper objectMapper,
                       EntityManager entityManager, AuditWriter auditWriter, AuditSnapshotReader auditSnapshotReader) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper       = objectMapper;
        this.entityManager      = entityManager;
        this.auditWriter = auditWriter;
        this.auditSnapshotReader = auditSnapshotReader;
    }

    /**
     * Intercepts all methods annotated with @AuditAction.
     * Captures before/after state and writes an AuditLogEntity row.
     */
    @Around("@annotation(com.academy.trafficviolationsystem.audit.AuditAction)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {

        // ── extract annotation metadata ───────────────────────────────────
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditAction annotation = method.getAnnotation(AuditAction.class);

        String action      = annotation.value();
        String entityType  = annotation.entityClass() != Object.class
                ? annotation.entityClass().getSimpleName()
                : null;
        boolean captureSnap = annotation.captureSnapshot();

        // ── extract actor from SecurityContext ────────────────────────────
        String actorUsername = "SYSTEM";
        UUID   actorId       = null;

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof UserPrincipal principal) {
                actorUsername = principal.getUsername();
                actorId       = principal.getId();
            }
        } catch (Exception e) {
            log.debug("Could not extract actor from SecurityContext: {}", e.getMessage());
        }

        // ── extract IP address ────────────────────────────────────────────
        String ipAddress = null;
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ipAddress = extractIp(request);
            }
        } catch (Exception e) {
            log.debug("Could not extract IP from request: {}", e.getMessage());
        }

        // ── extract entity ID from first UUID/Integer/Long argument ────────
        Object rawEntityId = extractRawEntityId(joinPoint.getArgs());
        UUID entityId = AuditIdCodec.toStorableEntityId(rawEntityId, entityType);
        // NOTE: entityId may be back-filled after proceed() for create-style
        // methods, where no ID argument exists until the entity is persisted.

        // ── capture BEFORE snapshot ───────────────────────────────────────
        // Fetched via EntityManager BEFORE the method runs, so it reflects
        // the pre-operation state. Uses rawEntityId (the entity's real PK
        // type), not entityId (which may be a synthetic UUID for non-UUID
        // PKs) — EntityManager.find() requires the actual PK type.
        String beforeSnapshot = null;
        if (captureSnap && rawEntityId != null && annotation.entityClass() != Object.class) {
            try {
                //Object beforeEntity = entityManager.find(annotation.entityClass(), rawEntityId);
                Object beforeEntity = auditSnapshotReader.snapshot(annotation.entityClass(), rawEntityId);
                beforeSnapshot = safeSerialize("before", beforeEntity);
            } catch (Exception e) {
                log.debug("Could not load before-snapshot for {}/{}: {}",
                        entityType, entityId, e.getMessage());
            }
        }

        // ── proceed with the actual method ────────────────────────────────
        // Wrapped so that a failed operation (e.g. a rejected confirmation,
        // a failed login) still produces an audit row instead of silently
        // skipping it — the exception is rethrown afterwards so callers see
        // the original failure exactly as before.
        Object result = null;
        Throwable failure = null;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            failure = t;
        }

        // ── capture AFTER snapshot ────────────────────────────────────────
        String afterSnapshot = null;
        if (captureSnap && failure == null && result != null) {
            afterSnapshot = safeSerialize("after", result);
        }

        // ── back-fill entity ID from the result if still unknown ──────────
        // Create-style methods (e.g. insert(UserCreateRequest)) have no
        // UUID/Integer/Long argument to extract from — the ID doesn't exist
        // until the entity is persisted inside proceed(). Without this,
        // entityId stays null and the insert into audit_log fails, since
        // entity_id is NOT NULL. Read it off the returned DTO's getId() via
        // reflection instead.
        if (entityId == null && failure == null && result != null) {
            Object resultId = extractIdFromResult(result);
            if (resultId != null) {
                entityId = AuditIdCodec.toStorableEntityId(resultId, entityType);
            }
        }

        // ── write audit row (non-blocking — never throw) ──────────────────
        // Fallback: some flows (e.g. AuthService.login()) don't have an
        // authenticated principal until partway through the method body —
        // SecurityContextHolder may only be populated after proceed()
        // returns. Re-check once if we didn't get an actor beforehand.
        if ("SYSTEM".equals(actorUsername)) {
            try {
                Authentication authAfter = SecurityContextHolder.getContext().getAuthentication();
                if (authAfter != null && authAfter.isAuthenticated()
                        && authAfter.getPrincipal() instanceof UserPrincipal principalAfter) {
                    actorUsername = principalAfter.getUsername();
                    actorId       = principalAfter.getId();
                }
            } catch (Exception e) {
                log.debug("Could not extract actor from SecurityContext (post-proceed): {}", e.getMessage());
            }
        }

        final String finalActorUsername = actorUsername;
        final UUID   finalActorId       = actorId;
        final String finalIp            = ipAddress;
        final Throwable finalFailure    = failure;

        try {
            AuditLogEntity auditLog = new AuditLogEntity();
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setActorId(finalActorId);
            auditLog.setActorUsername(finalActorUsername);
            auditLog.setIpAddress(finalIp);
            auditLog.setBeforeSnapshot(beforeSnapshot);
            auditLog.setAfterSnapshot(afterSnapshot);
            auditLog.setDescription(buildDescription(
                    action, entityType, entityId, finalActorUsername, finalFailure));
            auditLog.setOccurredAt(LocalDateTime.now());
            //auditLogRepository.save(auditLog);
            auditWriter.write(auditLog);
        } catch (Exception e) {
            // NEVER let audit failure break the business operation
            log.error("Failed to write audit log for action {} on {}/{}: {}",
                    action, entityType, entityId, e.getMessage());
        }

        if (failure != null) {
            throw failure;
        }
        return result;
    }

    // ── private helpers ───────────────────────────────────────────────────

    /**
     * Scans method arguments for the first UUID, Integer, or Long parameter.
     * By convention service methods have the entity ID as their first argument.
     * Returns the raw value as-is (needed for EntityManager.find(), which
     * requires the entity's real PK type) — convert to a storable UUID with
     * toStorableEntityId() separately.
     */
    private Object extractRawEntityId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof UUID || arg instanceof Integer || arg instanceof Long) {
                return arg;
            }
        }
        return null;
    }

    /**
     * Attempts to read an ID off the method's return value via getId(),
     * for use when no ID argument was available (create-style methods).
     * Returns null if there is no getId() method, it returns null, or
     * anything goes wrong — this is best-effort, never fatal.
     */
    private Object extractIdFromResult(Object result) {
        try {
            Method getId = result.getClass().getMethod("getId");
            return getId.invoke(result);
        } catch (Exception e) {
            log.debug("Could not extract id from result via reflection: {}", e.getMessage());
            return null;
        }
    }

    private String safeSerialize(String phase, Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.debug("Could not serialise {} snapshot: {}", phase, e.getMessage());
            return "{\"error\":\"serialization failed\"}";
        }
    }

    private String buildDescription(String action, String entityType, UUID entityId,
                                    String actor, Throwable failure) {
        String base = String.format("%s performed %s on %s/%s",
                actor,
                action,
                entityType != null ? entityType : "unknown",
                entityId != null ? entityId.toString().substring(0, 8) + "…" : "null"
        );
        if (failure != null) {
            return base + String.format(" — FAILED (%s: %s)",
                    failure.getClass().getSimpleName(), failure.getMessage());
        }
        return base;
    }

    /**
     * Extracts the real client IP, handling reverse proxy X-Forwarded-For headers.
     */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can be a comma-separated list — take the first (original client)
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}