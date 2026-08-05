package com.academy.trafficviolationsystem.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    /**
     * Full audit history for a single record — e.g. all changes to FineEntity with id X.
     * Used by GET /api/audit/entity/{type}/{entityId}.
     */
    List<AuditLogEntity> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
            String entityType, UUID entityId);

    /**
     * All actions performed by a specific user — for officer accountability reports.
     * Used by GET /api/audit/actor/{userId}.
     */
    List<AuditLogEntity> findByActorIdOrderByOccurredAtDesc(UUID actorId);

    /**
     * All entries for a given action type — e.g. all CANCEL_FINE events.
     */
    List<AuditLogEntity> findByActionOrderByOccurredAtDesc(String action);
}
