package com.academy.trafficviolationsystem.camera;

import com.academy.trafficviolationsystem.core.mappers.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CameraEventMapper extends BaseMapper<CameraEventEntity, CameraEventDto> {

    @Override
    @Mapping(target = "cameraId",   source = "camera.id")
    @Mapping(target = "cameraName", source = "camera.name")
    CameraEventDto toDto(CameraEventEntity entity);
}
