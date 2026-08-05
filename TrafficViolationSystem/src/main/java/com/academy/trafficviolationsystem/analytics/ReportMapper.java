package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.core.mappers.BaseMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for the reports sub-module.
 *
 * Extends BaseMapper (read-only — reports are never created or updated
 * from a request body; they are created programmatically by ReportService
 * and mutated only by ReportGenerationService).
 *
 * filePath is intentionally excluded from the DTO mapping — server
 * filesystem paths must never be exposed in API responses. Clients
 * use GET /api/reports/{id}/download to stream the file.
 *
 * isReady is computed in @AfterMapping from status == DONE.
 */
@Mapper(componentModel = "spring")
public interface ReportMapper extends BaseMapper<GeneratedReportEntity, ReportDto> {

    @Override
    @Mapping(target = "requestedById",       source = "requestedBy.id")
    @Mapping(target = "requestedByUsername", source = "requestedBy.username")
    @Mapping(target = "isReady",             ignore = true) // set in @AfterMapping
    ReportDto toDto(GeneratedReportEntity entity);

    @AfterMapping
    default void computeIsReady(GeneratedReportEntity entity, @MappingTarget ReportDto dto) {
        dto.setReady(entity.getStatus() == ReportStatus.DONE);
    }
}
