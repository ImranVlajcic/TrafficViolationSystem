package com.academy.trafficviolationsystem.core.services;

import com.academy.trafficviolationsystem.core.entities.AbstractEntity;
import com.academy.trafficviolationsystem.core.mappers.BaseCRUDMapper;
import com.academy.trafficviolationsystem.core.model.BaseSearchObject;

/**
 * Extends BaseService with insert and update operations.
 *
 * The four lifecycle hooks are the most important feature here.
 * Override them in your domain service to inject business logic
 * without touching the save/update flow itself:
 *
 *   beforeInsert — validate business rules before the entity is saved
 *   afterInsert  — trigger side effects after save (generate PDF, send notification)
 *   beforeUpdate — validate rules or capture old state before patching
 *   afterUpdate  — trigger side effects after update
 *
 */
public interface BaseCRUDService<
        E extends AbstractEntity,
        DTO,
        SObj extends BaseSearchObject<?>,
        TInsert,
        TUpdate,
        T>
        extends BaseService<E, DTO, SObj, T> {

    @Override
    BaseCRUDMapper<E, DTO, TInsert, TUpdate> getMapper();

    /**
     * Persist a new entity from a create request.
     *
     * Flow:
     *   1. mapper.toEntityFromInsert(request)  — map fields onto a new entity
     *   2. beforeInsert(request, entity)        — your business logic hook
     *   3. repository.save(entity)              — write to DB
     *   4. afterInsert(request, entity)         — your side-effects hook
     *   5. mapper.toDto(entity)                 — return the saved state as DTO
     */
    default DTO insert(TInsert request) {
        E entity = getMapper().toEntityFromInsert(request);
        beforeInsert(request, entity);
        entity = getRepository().save(entity);
        afterInsert(request, entity);
        return getMapper().toDto(entity);
    }

    /**
     * Update an existing entity from an update request.
     *
     * Flow:
     *   1. findEntityById(id)                   — load existing entity (throws 404 if missing)
     *   2. beforeUpdate(request, entity)         — your business logic hook (capture old state here)
     *   3. mapper.toEntityFromUpdate(request, entity) — patch entity fields in place
     *   4. repository.save(entity)               — write to DB
     *   5. afterUpdate(request, entity)           — your side-effects hook
     *   6. mapper.toDto(entity)                  — return updated state as DTO
     */
    default DTO update(T id, TUpdate request) {
        E entity = findEntityById(id);
        beforeUpdate(request, entity);
        getMapper().toEntityFromUpdate(request, entity);
        entity = getRepository().save(entity);
        afterUpdate(request, entity);
        return getMapper().toDto(entity);
    }

    // ── lifecycle hooks — override these in your service ─────────────────

    /** Called after mapping, before save. Validate business rules here. */
    default void beforeInsert(TInsert request, E entity) {}

    /** Called after save. Trigger PDFs, notifications, point deductions here. */
    default void afterInsert(TInsert request, E entity) {}

    /** Called before the update mapper runs. Capture old state or reject here. */
    default void beforeUpdate(TUpdate request, E entity) {}

    /** Called after update save. Trigger side-effects on update here. */
    default void afterUpdate(TUpdate request, E entity) {}
}
