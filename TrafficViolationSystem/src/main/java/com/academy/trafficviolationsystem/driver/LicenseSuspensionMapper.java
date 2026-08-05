package com.academy.trafficviolationsystem.driver;

import com.academy.trafficviolationsystem.core.mappers.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps LicenseSuspensionEntity → LicenseSuspensionDto.
 * Read-only — no insert/update mapping needed since suspensions
 * are created exclusively by DriverService domain operations, never
 * via a mapper from a generic request body.
 */
@Mapper(componentModel = "spring")
public interface LicenseSuspensionMapper extends BaseMapper<LicenseSuspensionEntity, LicenseSuspensionDto> {

    @Override
    @Mapping(target = "suspendedById",       source = "suspendedBy.id")
    @Mapping(target = "suspendedByUsername", source = "suspendedBy.username")
    LicenseSuspensionDto toDto(LicenseSuspensionEntity entity);
}
