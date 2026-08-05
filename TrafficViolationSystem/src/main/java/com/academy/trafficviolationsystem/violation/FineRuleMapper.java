package com.academy.trafficviolationsystem.violation;

import com.academy.trafficviolationsystem.core.mappers.BaseCRUDMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface FineRuleMapper extends BaseCRUDMapper<FineRuleEntity, FineRuleDto, FineRuleCreateRequest, FineRuleUpdateRequest> {

    @Override
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "active",  constant = "true")
    @Mapping(target = "created",   ignore = true)
    @Mapping(target = "updated",   ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    FineRuleEntity toEntityFromInsert(FineRuleCreateRequest request);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "violationType",   ignore = true) // immutable after creation
    @Mapping(target = "created",         ignore = true)
    @Mapping(target = "updated",         ignore = true)
    @Mapping(target = "createdBy",       ignore = true)
    @Mapping(target = "updatedBy",       ignore = true)
    @Mapping(target = "deletedAt",       ignore = true)
    void toEntityFromUpdate(FineRuleUpdateRequest request, @MappingTarget FineRuleEntity entity);

    @Override
    FineRuleDto toDto(FineRuleEntity entity);
}
