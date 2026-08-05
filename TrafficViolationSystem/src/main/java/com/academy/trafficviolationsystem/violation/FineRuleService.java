package com.academy.trafficviolationsystem.violation;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.auth.DuplicateResourceException;
import com.academy.trafficviolationsystem.core.services.BaseCRUDService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for fine rule management.
 *
 * FineRuleEntity rows are read on every fine issuance, so findActiveByType()
 * is cached with @Cacheable. The cache is evicted on every insert or update
 * so stale amounts are never used.
 *
 * Called by FineService.beforeInsert() to look up the rule for a violation type.
 */
@Service
@Transactional
public class FineRuleService implements BaseCRUDService<
        FineRuleEntity, FineRuleDto, FineRuleSearchObject, FineRuleCreateRequest, FineRuleUpdateRequest, Integer> {

    private final FineRuleRepository fineRuleRepository;
    private final FineRuleMapper     fineRuleMapper;
    private final EntityManager      entityManager;

    public FineRuleService(FineRuleRepository fineRuleRepository,
                           FineRuleMapper fineRuleMapper,
                           EntityManager entityManager) {
        this.fineRuleRepository = fineRuleRepository;
        this.fineRuleMapper     = fineRuleMapper;
        this.entityManager      = entityManager;
    }

    // ── BaseCRUDService wiring ────────────────────────────────────────────

    @Override public FineRuleRepository    getRepository()    { return fineRuleRepository; }
    @Override public EntityManager         getEntityManager() { return entityManager;      }
    @Override public FineRuleMapper        getMapper()        { return fineRuleMapper;     }
    @Override public Class<FineRuleEntity> getEntityClass()   { return FineRuleEntity.class; }

    // ── lifecycle hooks ───────────────────────────────────────────────────

    @Override
    @AuditAction(value = "CREATE_FINE_RULE", entityClass = FineRuleEntity.class)
    public FineRuleDto insert(FineRuleCreateRequest request) {
        return BaseCRUDService.super.insert(request);
    }

    @Override
    @AuditAction(value = "UPDATE_FINE_RULE", entityClass = FineRuleEntity.class)
    public FineRuleDto update(Integer id, FineRuleUpdateRequest request) {
        return BaseCRUDService.super.update(id, request);
    }

    @Override
    @CacheEvict(value = "fineRules", allEntries = true)
    public void beforeInsert(FineRuleCreateRequest request, FineRuleEntity entity) {
        if (fineRuleRepository.existsByViolationTypeAndIsActiveTrue(request.getViolationType())) {
            throw new DuplicateResourceException(
                    "An active fine rule already exists for type: " + request.getViolationType() +
                            ". Deactivate the existing rule before creating a new one.");
        }
        if (request.getMinAmount() != null && request.getMaxAmount() != null) {
            if (request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
                throw new BadRequestException("minAmount must not exceed maxAmount");
            }
        }
    }

    @Override
    @CacheEvict(value = "fineRules", allEntries = true)
    public void beforeUpdate(FineRuleUpdateRequest request, FineRuleEntity entity) {
        if (request.getMinAmount() != null && request.getMaxAmount() != null) {
            if (request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
                throw new BadRequestException("minAmount must not exceed maxAmount");
            }
        }
    }

    // ── search ────────────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                            FineRuleSearchObject searchObj,
                                            Root<FineRuleEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getViolationType() != null) {
            predicates.add(cb.equal(root.get("violationType"), searchObj.getViolationType()));
        }
        if (searchObj.getIsActive() != null) {
            predicates.add(cb.equal(root.get("isActive"), searchObj.getIsActive()));
        }
        return predicates;
    }

    // ── domain lookup (called by FineService) ─────────────────────────────

    /**
     * Returns the active rule for a given violation type.
     * Cached to avoid a DB hit on every fine issuance.
     * Called by FineService.beforeInsert().
     */
    @Cacheable(value = "fineRules", key = "#violationType")
    @Transactional(readOnly = true)
    public FineRuleEntity findActiveByType(ViolationType violationType) {
        return fineRuleRepository.findByViolationTypeAndIsActiveTrue(violationType)
                .orElseThrow(() -> new NotFoundException(
                    "No active fine rule configured for violation type: " + violationType +
                    ". Ask an admin to create one via POST /api/fine-rules."));
    }
}
