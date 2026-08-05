package com.academy.trafficviolationsystem.rodezone;

import com.academy.trafficviolationsystem.core.mappers.BaseCRUDMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for the road zone module.
 *
 * Extends BaseCRUDMapper<Entity, Dto, TInsert, TUpdate> — 4 generics required.
 * TInsert = RoadZoneCreateRequest, TUpdate = RoadZoneUpdateRequest.
 *
 * Audit field names match BaseEntity: created, updated, createdBy, updatedBy, deletedAt.
 *
 * cameraCount is ignored here — populated by RoadZoneService.toDtoWithCameraCount()
 * via a CameraRepository.countByZoneId() call after mapping.
 */
@Mapper(componentModel = "spring")
public interface RoadZoneMapper extends BaseCRUDMapper<
        RoadZoneEntity, RoadZoneDto, RoadZoneCreateRequest, RoadZoneUpdateRequest> {

    @Override
    @Mapping(target = "cameraCount", ignore = true)
    RoadZoneDto toDto(RoadZoneEntity entity);

    @Override
    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "active",   constant = "true")
    @Mapping(target = "created",    ignore = true)
    @Mapping(target = "updated",    ignore = true)
    @Mapping(target = "createdBy",  ignore = true)
    @Mapping(target = "updatedBy",  ignore = true)
    @Mapping(target = "deletedAt",  ignore = true)
    RoadZoneEntity toEntityFromInsert(RoadZoneCreateRequest request);

    /**
     * Partial update — IGNORE strategy means null fields leave entity unchanged.
     * isActive CAN be updated here (driver can deactivate via RoadZoneUpdateRequest).
     * Argument order: (request, @MappingTarget entity) — matches BaseCRUDMapper.
     */
    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "created",   ignore = true)
    @Mapping(target = "updated",   ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void toEntityFromUpdate(RoadZoneUpdateRequest request,
                             @MappingTarget RoadZoneEntity entity);
}
