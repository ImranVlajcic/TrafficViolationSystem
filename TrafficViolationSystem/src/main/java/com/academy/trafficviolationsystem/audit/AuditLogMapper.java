package com.academy.trafficviolationsystem.audit;

import com.academy.trafficviolationsystem.core.mappers.BaseMapper;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for the audit module.
 * Simple toDto() — all fields map directly with no transformation needed.
 * No insert/update mapper — AuditLogEntity is written only by AuditAspect.
 */
@Mapper(componentModel = "spring")
public interface AuditLogMapper extends BaseMapper<AuditLogEntity, AuditLogDto> {

    @Override
    AuditLogDto toDto(AuditLogEntity entity);
}
