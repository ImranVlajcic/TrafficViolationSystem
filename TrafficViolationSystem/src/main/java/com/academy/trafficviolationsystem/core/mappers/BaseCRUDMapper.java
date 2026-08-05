package com.academy.trafficviolationsystem.core.mappers;

import com.academy.trafficviolationsystem.core.entities.AbstractEntity;
import org.mapstruct.MappingTarget;

/**
 * Base mapper interface for converting Insert and Update requests to Entities.
 *
 * Two existing methods handle all conversion for existing modules.
 *
 */

public interface BaseCRUDMapper<E extends AbstractEntity, DTO, InsertRequest, UpdateRequest>
        extends BaseMapper<E, DTO> {

    E toEntityFromInsert(InsertRequest request);

    void toEntityFromUpdate(UpdateRequest request, @MappingTarget E entity);
}
