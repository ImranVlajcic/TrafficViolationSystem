package com.academy.trafficviolationsystem.notification;

import com.academy.trafficviolationsystem.core.mappers.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for the notification module.
 *
 * NotificationEntity is always built programmatically by NotificationService —
 * never from a request body. So only toDto() is needed here, not BaseCRUDMapper.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper extends BaseMapper<NotificationEntity, NotificationDto> {

    @Override
    @Mapping(target = "userId", source = "user.id")
    NotificationDto toDto(NotificationEntity entity);
}
