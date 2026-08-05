package com.academy.trafficviolationsystem.audit;

import com.academy.trafficviolationsystem.core.services.BaseService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Read-only service for the audit log.
 *
 * Implements BaseService (not BaseCRUDService) — there is no insert/update/delete
 * exposed via this service. The write path is exclusively in AuditAspect.
 *
 * Provides:
 *   search()          — paginated search with all AuditSearchObject filters
 *   findById()        — single entry by ID
 *   getForEntity()    — full history for one record (e.g. all changes to FineEntity X)
 *   getForActor()     — all actions performed by a specific user
 */
@Service
@Transactional(readOnly = true)
public class AuditLogService implements BaseService<
        AuditLogEntity, AuditLogDto, AuditSearchObject, UUID> {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper     auditLogMapper;
    private final EntityManager      entityManager;

    public AuditLogService(AuditLogRepository auditLogRepository,
                            AuditLogMapper auditLogMapper,
                            EntityManager entityManager) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper     = auditLogMapper;
        this.entityManager      = entityManager;
    }

    // ── BaseService wiring ────────────────────────────────────────────────

    @Override public CrudRepository<AuditLogEntity, UUID> getRepository()    { return auditLogRepository; }
    @Override public EntityManager                        getEntityManager() { return entityManager;      }
    @Override public AuditLogMapper                       getMapper()        { return auditLogMapper;     }
    @Override public Class<AuditLogEntity>                getEntityClass()   { return AuditLogEntity.class; }

    // ── search filters ────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                            AuditSearchObject searchObj,
                                            Root<AuditLogEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getAction() != null && !searchObj.getAction().isBlank()) {
            predicates.add(cb.equal(root.get("action"), searchObj.getAction()));
        }
        if (searchObj.getEntityType() != null && !searchObj.getEntityType().isBlank()) {
            predicates.add(cb.equal(root.get("entityType"), searchObj.getEntityType()));
        }
        if (searchObj.getEntityId() != null) {
            predicates.add(cb.equal(root.get("entityId"), searchObj.getEntityId()));
        }
        if (searchObj.getActorId() != null) {
            predicates.add(cb.equal(root.get("actorId"), searchObj.getActorId()));
        }
        if (searchObj.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                root.get("occurredAt"), searchObj.getFromDate().atStartOfDay()));
        }
        if (searchObj.getToDate() != null) {
            predicates.add(cb.lessThan(
                root.get("occurredAt"), searchObj.getToDate().plusDays(1).atStartOfDay()));
        }
        return predicates;
    }

    // ── domain read helpers ───────────────────────────────────────────────

    /**
     * Returns the full change history for a single domain record.
     * e.g. getForEntity("FineEntity", fineId) returns every audit entry
     * for that specific fine — issuance, disputes, cancellation, etc.
     *
     * Used by GET /api/audit/entity/{type}/{entityId}
     */
    public List<AuditLogDto> getForEntity(String entityType, UUID entityId) {
        return auditLogMapper.toDtoList(
            auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
                entityType, entityId));
    }

    /**
     * Returns all actions performed by a specific user.
     * Used by GET /api/audit/actor/{userId} for officer accountability.
     */
    public List<AuditLogDto> getForActor(UUID actorId) {
        return auditLogMapper.toDtoList(
            auditLogRepository.findByActorIdOrderByOccurredAtDesc(actorId));
    }
}
