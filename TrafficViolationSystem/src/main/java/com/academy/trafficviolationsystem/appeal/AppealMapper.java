package com.academy.trafficviolationsystem.appeal;

import com.academy.trafficviolationsystem.core.mappers.BaseCRUDMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * MapStruct mapper for the appeal module.
 *
 * toEntityFromInsert:
 *   appealNumber, status, submittedAt, reviewedAt, reviewNotes, reviewedBy,
 *   violation, driver, and fineId are all ignored here — AppealService.beforeInsert()
 *   sets them with proper business logic (e.g. loading ViolationEntity,
 *   resolving the driver from the principal, generating the appeal number).
 *
 * toEntityFromUpdate:
 *   IGNORE strategy — only reason and evidenceUrl can be changed.
 *   All other fields are immutable once filed.
 *
 * toDto (@AfterMapping):
 *   driverFullName    — assembled from driver.firstName + driver.lastName
 *   reviewedByFullName — assembled from reviewedBy.firstName + reviewedBy.lastName
 *   daysOpen          — ChronoUnit.DAYS between submittedAt and now
 *   violationReference — left null here, populated by AppealService.toDtoWithDetails()
 */
@Mapper(componentModel = "spring")
public interface AppealMapper extends BaseCRUDMapper<
        ViolationAppealEntity, AppealDto, AppealCreateRequest, AppealUpdateRequest> {

    @Override
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "appealNumber",  ignore = true) // generated in beforeInsert
    @Mapping(target = "status",        ignore = true) // set to SUBMITTED in beforeInsert
    @Mapping(target = "submittedAt",   ignore = true) // set to now() in beforeInsert
    @Mapping(target = "reviewedAt",    ignore = true)
    @Mapping(target = "reviewNotes",   ignore = true)
    @Mapping(target = "reviewedBy",    ignore = true)
    @Mapping(target = "violation",     ignore = true) // loaded in beforeInsert
    @Mapping(target = "driver",        ignore = true) // resolved from principal in beforeInsert
    @Mapping(target = "fineId",        ignore = true) // resolved in beforeInsert
    @Mapping(target = "created",       ignore = true)
    @Mapping(target = "updated",       ignore = true)
    @Mapping(target = "createdBy",     ignore = true)
    @Mapping(target = "updatedBy",     ignore = true)
    @Mapping(target = "deletedAt",     ignore = true)
    ViolationAppealEntity toEntityFromInsert(AppealCreateRequest request);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "appealNumber",  ignore = true) // immutable
    @Mapping(target = "status",        ignore = true) // changed only via dedicated endpoints
    @Mapping(target = "submittedAt",   ignore = true) // immutable
    @Mapping(target = "reviewedAt",    ignore = true) // managed by review operations
    @Mapping(target = "reviewNotes",   ignore = true) // managed by review operations
    @Mapping(target = "reviewedBy",    ignore = true) // managed by review operations
    @Mapping(target = "violation",     ignore = true) // immutable
    @Mapping(target = "driver",        ignore = true) // immutable
    @Mapping(target = "fineId",        ignore = true) // immutable
    @Mapping(target = "created",       ignore = true)
    @Mapping(target = "updated",       ignore = true)
    @Mapping(target = "createdBy",     ignore = true)
    @Mapping(target = "updatedBy",     ignore = true)
    @Mapping(target = "deletedAt",     ignore = true)
    void toEntityFromUpdate(AppealUpdateRequest request, @MappingTarget ViolationAppealEntity entity);

    @Override
    @Mapping(target = "violationId",       source = "violation.id")
    @Mapping(target = "driverId",          source = "driver.id")
    @Mapping(target = "driverFullName",    ignore = true) // set in @AfterMapping
    @Mapping(target = "driverLicenseNumber", source = "driver.licenseNumber")
    @Mapping(target = "reviewedById",      source = "reviewedBy.id")
    @Mapping(target = "reviewedByFullName",ignore = true) // set in @AfterMapping
    @Mapping(target = "violationReference",ignore = true) // set by AppealService
    @Mapping(target = "daysOpen",          ignore = true) // set in @AfterMapping
    AppealDto toDto(ViolationAppealEntity entity);

    @AfterMapping
    default void computeDerivedFields(ViolationAppealEntity entity,
                                       @MappingTarget AppealDto dto) {
        // driverFullName
        if (entity.getDriver() != null) {
            dto.setDriverFullName(
                entity.getDriver().getFirstName() + " " + entity.getDriver().getLastName());
        }

        // reviewedByFullName
        if (entity.getReviewedBy() != null) {
            dto.setReviewedByFullName(
                entity.getReviewedBy().getFirstName() + " " + entity.getReviewedBy().getLastName());
        }

        // daysOpen — how long the appeal has been waiting
        if (entity.getSubmittedAt() != null) {
            dto.setDaysOpen(ChronoUnit.DAYS.between(entity.getSubmittedAt(), LocalDateTime.now()));
        }
    }
}
