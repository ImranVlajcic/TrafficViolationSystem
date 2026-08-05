package com.academy.trafficviolationsystem.configuration;

import com.academy.trafficviolationsystem.core.mappers.BaseCRUDMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for the configuration module.
 *
 * Extends BaseCRUDMapper<Entity, Dto, TInsert, TUpdate>.
 * Both TInsert and TUpdate are SystemConfigUpdateRequest because
 * there is no separate create request — HTTP creation is blocked.
 *
 * toEntityFromInsert is intentionally unsupported (rows are Flyway-seeded).
 * toEntityFromUpdate applies only configValue and description — all other
 * fields are immutable after seeding.
 *
 * Audit field names match BaseEntity: 'created', 'updated', 'createdBy', 'updatedBy'.
 */
@Mapper(componentModel = "spring")
public interface SystemConfigMapper extends BaseCRUDMapper<
        SystemConfigEntity, SystemConfigDto,
        SystemConfigUpdateRequest, SystemConfigUpdateRequest> {

    @Override
    SystemConfigDto toDto(SystemConfigEntity entity);

    /**
     * Not supported — SystemConfig rows are seeded by Flyway.
     * Returns null to satisfy the interface; SystemConfigService.beforeInsert()
     * throws UnsupportedOperationException before this is ever called.
     */
    @Override
    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "configKey",  ignore = true)
    @Mapping(target = "dataType",   ignore = true)
    @Mapping(target = "category",   ignore = true)
    @Mapping(target = "editable", ignore = true)
    @Mapping(target = "created",    ignore = true)
    @Mapping(target = "updated",    ignore = true)
    @Mapping(target = "createdBy",  ignore = true)
    @Mapping(target = "updatedBy",  ignore = true)
    @Mapping(target = "deletedAt",  ignore = true)
    SystemConfigEntity toEntityFromInsert(SystemConfigUpdateRequest request);

    /**
     * Partial update — only configValue and description are writable.
     * configKey, dataType, category, and isEditable are immutable after seeding.
     * IGNORE strategy means null fields in the request leave the entity unchanged.
     */
    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "configKey",  ignore = true)
    @Mapping(target = "dataType",   ignore = true)
    @Mapping(target = "category",   ignore = true)
    @Mapping(target = "editable", ignore = true)
    @Mapping(target = "created",    ignore = true)
    @Mapping(target = "updated",    ignore = true)
    @Mapping(target = "createdBy",  ignore = true)
    @Mapping(target = "updatedBy",  ignore = true)
    @Mapping(target = "deletedAt",  ignore = true)
    void toEntityFromUpdate(SystemConfigUpdateRequest request,
                             @MappingTarget SystemConfigEntity entity);
}
