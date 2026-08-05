package com.academy.trafficviolationsystem.core.controllers;

import com.academy.trafficviolationsystem.core.entities.AbstractEntity;
import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import com.academy.trafficviolationsystem.core.services.BaseCRUDService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Extends BaseController with write endpoints (create and update).
 *
 * Any controller that needs full CRUD implements this interface.
 * Read-only resources (e.g. AuditLogController — never written via HTTP)
 * can implement the plain BaseController instead.
 *
 * Delete is intentionally absent at the base level. "Deleting" in this
 * system means soft-delete via AbstractEntity.preRemove(), which is triggered
 * by calling repository.delete(entity) in the service. Add a
 * @DeleteMapping("{id}") to individual controllers only where required.
 *
 */
public interface BaseCRUDController<
        E extends AbstractEntity,
        DTO,
        SObj extends BaseSearchObject<?>,
        TInsert,
        TUpdate,
        T>
        extends BaseController<E, DTO, SObj, T> {

    @Override
    BaseCRUDService<E, DTO, SObj, TInsert, TUpdate, T> getService();

    /**
     * Create a new resource.
     * @Valid triggers Bean Validation on the request body — any @NotNull,
     * @Size, @Email, etc. annotations on TInsert are enforced before the
     * method body runs. Validation failures return 400 automatically via
     * GlobalExceptionHandler.
     *
     */
    @PostMapping
    default DTO create(@Valid @RequestBody TInsert payload) {
        return getService().insert(payload);
    }

    /**
     * Update an existing resource by its primary key.
     * Throws NotFoundException (404) if the record does not exist.
     */
    @PutMapping("{id}")
    default DTO update(@PathVariable T id, @Valid @RequestBody TUpdate payload) {
        return getService().update(id, payload);
    }
}
