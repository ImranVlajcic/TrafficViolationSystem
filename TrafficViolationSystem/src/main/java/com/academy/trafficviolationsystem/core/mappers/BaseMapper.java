package com.academy.trafficviolationsystem.core.mappers;

import com.academy.trafficviolationsystem.core.entities.AbstractEntity;

import java.util.List;

/**
 * Base mapper interface for Entities into their respective DTOs.
 *
 * Two existing methods handle all conversion for existing modules
 * depending on if it is a single item or a list.
 *
 */

public interface BaseMapper<E extends AbstractEntity, DTO> {

    DTO toDto(E entity);

    default List<DTO> toDtoList(List<E> entities) {
        return entities.stream().map(this::toDto).toList();
    }
}
