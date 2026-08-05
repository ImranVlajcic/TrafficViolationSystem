package com.academy.trafficviolationsystem.camera;

import com.academy.trafficviolationsystem.core.mappers.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CameraMaintenanceLogMapper extends BaseMapper<CameraMaintenanceLogEntity, CameraMaintenanceLogDto> {

    @Override
    @Mapping(target = "cameraId",            source = "camera.id")
    @Mapping(target = "performedById",        source = "performedBy.id")
    @Mapping(target = "performedByUsername",  source = "performedBy.username")
    CameraMaintenanceLogDto toDto(CameraMaintenanceLogEntity entity);
}
