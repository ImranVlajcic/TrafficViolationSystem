package com.academy.trafficviolationsystem.camera;

import com.academy.trafficviolationsystem.core.mappers.BaseCRUDMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring")
public interface CameraMapper extends BaseCRUDMapper<CameraEntity, CameraDto, CameraCreateRequest, CameraUpdateRequest> {

    @Override
    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "online",           constant = "false")
    @Mapping(target = "active",           constant = "true")
    @Mapping(target = "lastHeartbeatAt",    ignore = true)
    @Mapping(target = "created",            ignore = true)
    @Mapping(target = "updated",            ignore = true)
    @Mapping(target = "createdBy",          ignore = true)
    @Mapping(target = "updatedBy",          ignore = true)
    @Mapping(target = "deletedAt",          ignore = true)
    CameraEntity toEntityFromInsert(CameraCreateRequest request);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "serialNumber",       ignore = true) // immutable
    @Mapping(target = "mqttTopic",          ignore = true) // immutable
    @Mapping(target = "latitude",           ignore = true) // location changes via dedicated endpoint
    @Mapping(target = "longitude",          ignore = true)
    @Mapping(target = "online",           ignore = true) // managed by heartbeat job
    @Mapping(target = "lastHeartbeatAt",    ignore = true) // managed by heartbeat listener
    @Mapping(target = "created",            ignore = true)
    @Mapping(target = "updated",            ignore = true)
    @Mapping(target = "createdBy",          ignore = true)
    @Mapping(target = "updatedBy",          ignore = true)
    @Mapping(target = "deletedAt",          ignore = true)
    void toEntityFromUpdate(CameraUpdateRequest request, @MappingTarget CameraEntity entity);

    @Override
    @Mapping(target = "minutesSinceHeartbeat", ignore = true) // set in @AfterMapping
    CameraDto toDto(CameraEntity entity);

    @AfterMapping
    default void computeDerived(CameraEntity entity, @MappingTarget CameraDto dto) {
        if (entity.getLastHeartbeatAt() != null) {
            dto.setMinutesSinceHeartbeat(
                ChronoUnit.MINUTES.between(entity.getLastHeartbeatAt(), LocalDateTime.now())
            );
        }
    }
}
