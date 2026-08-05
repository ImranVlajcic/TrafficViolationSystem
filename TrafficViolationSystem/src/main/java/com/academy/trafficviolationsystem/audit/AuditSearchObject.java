package com.academy.trafficviolationsystem.audit;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Search/filter parameters for GET /api/audit.
 *
 * All fields optional. Null fields are ignored in AuditLogService.additionalFilter().
 *
 * Example requests:
 *   GET /api/audit?entityType=FineEntity&entityId=<uuid>
 *   GET /api/audit?action=CANCEL_FINE&fromDate=2025-01-01
 *   GET /api/audit?actorId=<uuid>
 */
@Getter
@Setter
public class AuditSearchObject extends BaseSearchObject<UUID> {

    /** Filter by action name (exact match). e.g. "CONFIRM_VIOLATION" */
    private String action;

    /** Filter by entity class name. e.g. "FineEntity" */
    private String entityType;

    /** Filter to all audit entries for a specific record. */
    private UUID entityId;

    /** Filter to all actions performed by a specific user. */
    private UUID actorId;

    /** occurredAt date range — start (inclusive). */
    private LocalDate fromDate;

    /** occurredAt date range — end (inclusive). */
    private LocalDate toDate;
}
