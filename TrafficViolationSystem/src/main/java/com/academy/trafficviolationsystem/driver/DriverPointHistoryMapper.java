package com.academy.trafficviolationsystem.driver;

import com.academy.trafficviolationsystem.core.mappers.BaseMapper;
import org.mapstruct.Mapper;

/**
 * Maps DriverPointHistoryEntity → DriverPointHistoryDto.
 * Read-only — entries are written directly by DriverService, never via a mapper.
 */
@Mapper(componentModel = "spring")
public interface DriverPointHistoryMapper extends BaseMapper<DriverPointHistoryEntity, DriverPointHistoryDto> {

    @Override
    DriverPointHistoryDto toDto(DriverPointHistoryEntity entity);
}
