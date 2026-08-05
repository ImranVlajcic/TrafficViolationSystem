package com.academy.trafficviolationsystem.core.controllers;

import com.academy.trafficviolationsystem.core.entities.AbstractEntity;
import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import com.academy.trafficviolationsystem.core.model.PagedResult;
import com.academy.trafficviolationsystem.core.services.BaseService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Base controller interface providing the two read-only endpoints
 * that every resource in this system exposes.
 *
 * Domain controller implements this interface and calls getService()
 * to return its own service. The two default methods below handle the
 * actual request — you get them for free without writing any code.
 *
 */
public interface BaseController<E extends AbstractEntity, DTO, SObj extends BaseSearchObject<?>, T> {

    BaseService<E, DTO, SObj, T> getService();

    /**
     * Paginated search endpoint.
     * All fields on the SObj are optional query parameters thanks to @ParameterObject.
     * Swagger UI will render each field as a separate input box.
     */
    @GetMapping
    default PagedResult<DTO> search(@ParameterObject SObj searchObj) {
        return getService().search(searchObj);
    }

    /**
     * Fetch a single record by its primary key.
     * Throws NotFoundException (HTTP 404) automatically if it does not exist.
     */
    @GetMapping("{id}")
    default DTO findById(@PathVariable T id) {
        return getService().findById(id);
    }
}
