package com.academy.trafficviolationsystem.fine;

import com.academy.trafficviolationsystem.core.mappers.BaseMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * MapStruct mapper for the fine module.
 *
 * FineEntity is never created from a request body — FineService builds it
 * programmatically from ViolationConfirmedEvent data and the FineRuleEntity.
 * So only toDto() is needed here, not the full BaseCRUDMapper.
 *
 * toDto (@AfterMapping):
 *   - earlyPayEligible: UNPAID and today <= issuedAt + earlyPayWindowDays
 *   - daysUntilDue: ChronoUnit.DAYS.between(today, dueDate) — negative if overdue
 *   - pdfReady: pdfPath is non-null
 *   - driverFullName: assembled from driver.firstName + driver.lastName
 *   - issuedByFullName: assembled from issuedBy.firstName + issuedBy.lastName
 *   - violationReference: fetched from the violation — left null here,
 *     populated by FineService.toDto() after a ViolationRepository lookup
 *     when the full reference string is needed.
 */
@Mapper(componentModel = "spring")
public interface FineMapper extends BaseMapper<FineEntity, FineDto> {

    @Override
    @Mapping(target = "driverId",           source = "driver.id")
    @Mapping(target = "driverFullName",      ignore = true) // set in @AfterMapping
    @Mapping(target = "driverLicenseNumber", source = "driver.licenseNumber")
    @Mapping(target = "issuedById",          source = "issuedBy.id")
    @Mapping(target = "issuedByFullName",    ignore = true) // set in @AfterMapping
    @Mapping(target = "pdfReady",            ignore = true) // set in @AfterMapping
    @Mapping(target = "earlyPayEligible",    ignore = true) // set in @AfterMapping
    @Mapping(target = "daysUntilDue",        ignore = true) // set in @AfterMapping
    @Mapping(target = "violationReference",  ignore = true) // set by FineService when needed
    FineDto toDto(FineEntity entity);

    @AfterMapping
    default void computeDerivedFields(FineEntity entity, @MappingTarget FineDto dto) {
        LocalDate today = LocalDate.now();

        // pdfReady
        dto.setPdfReady(entity.getPdfPath() != null);

        // daysUntilDue — negative means already overdue
        if (entity.getDueDate() != null) {
            dto.setDaysUntilDue(ChronoUnit.DAYS.between(today, entity.getDueDate()));
        }

        // earlyPayEligible — unpaid and still within the discount window
        if (entity.getStatus() == FineStatus.UNPAID && entity.getIssuedAt() != null) {
            LocalDate windowEnd = entity.getIssuedAt()
                    .toLocalDate()
                    .plusDays(entity.getEarlyPayWindowDays());
            dto.setEarlyPayEligible(!today.isAfter(windowEnd));
        }

        // driverFullName
        if (entity.getDriver() != null) {
            dto.setDriverFullName(
                entity.getDriver().getFirstName() + " " + entity.getDriver().getLastName());
        }

        // issuedByFullName
        if (entity.getIssuedBy() != null) {
            dto.setIssuedByFullName(
                entity.getIssuedBy().getFirstName() + " " + entity.getIssuedBy().getLastName());
        }
    }
}
