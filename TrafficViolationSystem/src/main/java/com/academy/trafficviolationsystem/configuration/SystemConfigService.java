package com.academy.trafficviolationsystem.configuration;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.config.ConfigCreationNotAllowedException;
import com.academy.trafficviolationsystem.core.exceptions.config.ConfigReadOnlyException;
import com.academy.trafficviolationsystem.core.exceptions.config.ConfigTypeMismatchException;
import com.academy.trafficviolationsystem.core.services.BaseCRUDService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for runtime system configuration.
 *
 * Implements BaseCRUDService — wired through the core BaseService/BaseCRUDService
 * pattern using EntityManager + CrudRepository exactly like every other module.
 *
 * HTTP creation and deletion are blocked — all rows come from Flyway migrations.
 * Only configValue and description are updatable via HTTP.
 *
 * Typed getters are @Cacheable("system-config") so the DB is hit only once
 * per key per cache TTL window (5 minutes, configured in core CacheConfig).
 * @CacheEvict(allEntries=true) on beforeUpdate() flushes the cache immediately
 * when an admin changes a value so callers see the new value right away.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SystemConfigService implements BaseCRUDService<
        SystemConfigEntity, SystemConfigDto,
        SystemConfigSearchObject,
        SystemConfigUpdateRequest, SystemConfigUpdateRequest,
        Integer> {

    private final SystemConfigRepository repo;
    private final SystemConfigMapper     mapper;
    private final EntityManager          entityManager;

    // ── BaseCRUDService wiring ────────────────────────────────────────────────

    @Override public SystemConfigRepository    getRepository()    { return repo;                    }
    @Override public EntityManager             getEntityManager() { return entityManager;           }
    @Override public SystemConfigMapper        getMapper()        { return mapper;                  }
    @Override public Class<SystemConfigEntity> getEntityClass()   { return SystemConfigEntity.class;}

    // ── lifecycle hooks ───────────────────────────────────────────────────────

    /**
     * HTTP creation is not allowed — all rows come from Flyway V3 migration.
     * Throws before the entity is ever saved.
     */
    @Override
    @AuditAction(value = "UPDATE_SYSTEM_CONFIG", entityClass = SystemConfigEntity.class)
    @CacheEvict(value = "system-config", allEntries = true)
    public SystemConfigDto update(Integer id, SystemConfigUpdateRequest request) {
        return BaseCRUDService.super.update(id, request);
    }

    @Override
    public void beforeInsert(SystemConfigUpdateRequest request, SystemConfigEntity entity) {
        throw new ConfigCreationNotAllowedException();
    }

    /**
     * Validates the update request before saving:
     *  1. isEditable must be true — read-only constants cannot be changed.
     *  2. configValue must parse correctly for the entity's declared dataType.
     */
    @Override
    public void beforeUpdate(SystemConfigUpdateRequest request, SystemConfigEntity entity) {
        if (!entity.isEditable()) {
            throw new ConfigReadOnlyException(entity.getConfigKey());
        }
        if (request.getConfigValue() != null) {
            validateValueType(entity.getDataType(), request.getConfigValue());
        }
    }

    // ── search filters ────────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                             SystemConfigSearchObject searchObj,
                                             Root<SystemConfigEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getCategory() != null && !searchObj.getCategory().isBlank()) {
            predicates.add(cb.equal(root.get("category"), searchObj.getCategory()));
        }
        if (searchObj.getDataType() != null) {
            predicates.add(cb.equal(root.get("dataType"), searchObj.getDataType()));
        }
        return predicates;
    }

    // ── typed getters (all cached) ────────────────────────────────────────────

    /**
     * Returns the config value as an Integer.
     * Throws NotFoundException if the key does not exist in the DB.
     * Throws IllegalStateException if the stored dataType is not INTEGER.
     */
    @Cacheable(value = "system-config", key = "#root.methodName + ':' + #key")
    @Transactional(readOnly = true)
    public Integer getInt(String key) {
        SystemConfigEntity entity = findByKeyOrThrow(key);
        assertType(entity, ConfigDataType.INTEGER);
        return Integer.parseInt(entity.getConfigValue().trim());
    }

    @Cacheable(value = "system-config", key = "#root.methodName + ':' + #key")
    @Transactional(readOnly = true)
    public String getString(String key) {
        return findByKeyOrThrow(key).getConfigValue();
    }

    @Cacheable(value = "system-config", key = "#root.methodName + ':' + #key")
    @Transactional(readOnly = true)
    public Boolean getBoolean(String key) {
        SystemConfigEntity entity = findByKeyOrThrow(key);
        assertType(entity, ConfigDataType.BOOLEAN);
        return Boolean.parseBoolean(entity.getConfigValue().trim());
    }

    @Cacheable(value = "system-config", key = "#root.methodName + ':' + #key")
    @Transactional(readOnly = true)
    public BigDecimal getDecimal(String key) {
        SystemConfigEntity entity = findByKeyOrThrow(key);
        assertType(entity, ConfigDataType.DECIMAL);
        return new BigDecimal(entity.getConfigValue().trim());
    }

    /** Returns raw JSON string — caller deserialises with ObjectMapper. */
    @Cacheable(value = "system-config", key = "#root.methodName + ':' + #key")
    @Transactional(readOnly = true)
    public String getJson(String key) {
        SystemConfigEntity entity = findByKeyOrThrow(key);
        assertType(entity, ConfigDataType.JSON);
        return entity.getConfigValue();
    }

    // ── convenience list by category ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SystemConfigDto> findByCategory(String category) {
        return repo.findByCategoryOrderByConfigKeyAsc(category)
                   .stream()
                   .map(mapper::toDto)
                   .collect(Collectors.toList());
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private SystemConfigEntity findByKeyOrThrow(String key) {
        return repo.findByConfigKey(key)
                .orElseThrow(() -> new NotFoundException(
                    "System config key not found: '" + key + "'. " +
                    "Ensure the Flyway V3 seed migration has run."));
    }

    private void assertType(SystemConfigEntity entity, ConfigDataType expected) {
        if (entity.getDataType() != expected) {
            throw new ConfigTypeMismatchException(entity.getConfigKey(), entity.getDataType(), expected);
        }
    }

    private void validateValueType(ConfigDataType dataType, String value) {
        try {
            switch (dataType) {
                case INTEGER -> Integer.parseInt(value.trim());
                case DECIMAL -> new BigDecimal(value.trim());
                case BOOLEAN -> {
                    String v = value.trim().toLowerCase();
                    if (!v.equals("true") && !v.equals("false")) {
                        throw new NumberFormatException("not a boolean");
                    }
                }
                case STRING, JSON -> { /* no type validation needed */ }
            }
        } catch (NumberFormatException ex) {
            throw new BadRequestException(
                "Value '" + value + "' is not valid for dataType " + dataType +
                ". Expected format: " + expectedFormat(dataType));
        }
    }

    private String expectedFormat(ConfigDataType dataType) {
        return switch (dataType) {
            case INTEGER -> "a whole number, e.g. 12";
            case DECIMAL -> "a decimal number, e.g. 0.10";
            case BOOLEAN -> "true or false";
            default      -> "a text string";
        };
    }
}
