package com.academy.trafficviolationsystem.driver;

import com.academy.trafficviolationsystem.core.mappers.BaseCRUDMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDate;

/**
 * MapStruct mapper for the driver module.
 *
 * Notable decisions:
 *
 *  toEntityFromInsert:
 *    - penaltyPoints, isSuspended, suspendedUntil all default to 0/false/null —
 *      they are managed by domain operations, never set from a create request.
 *    - user (the portal link) starts null — set via the link-account endpoint.
 *
 *  toEntityFromUpdate:
 *    - IGNORE strategy so null fields leave the entity unchanged.
 *    - licenseNumber and nationalId are ignored — they cannot be changed via HTTP.
 *    - Suspension fields are ignored — managed by domain operations only.
 *
 *  toDto (@AfterMapping):
 *    - licenseExpired is computed from licenseExpiresAt vs today.
 *    - userId is mapped from the nested user.id (if user is non-null).
 */
@Mapper(componentModel = "spring")
public interface DriverMapper extends BaseCRUDMapper<DriverEntity, DriverDto, DriverCreateRequest, DriverUpdateRequest> {

    @Override
    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "penaltyPoints",  constant = "0")
    @Mapping(target = "suspended",    constant = "false")
    @Mapping(target = "suspendedUntil", ignore = true)
    @Mapping(target = "user",           ignore = true)
    @Mapping(target = "created",        ignore = true)
    @Mapping(target = "updated",        ignore = true)
    @Mapping(target = "createdBy",      ignore = true)
    @Mapping(target = "updatedBy",      ignore = true)
    @Mapping(target = "deletedAt",      ignore = true)
    DriverEntity toEntityFromInsert(DriverCreateRequest request);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "licenseNumber",  ignore = true) // immutable after creation
    @Mapping(target = "nationalId",     ignore = true) // immutable after creation
    @Mapping(target = "dateOfBirth",    ignore = true) // immutable after creation
    @Mapping(target = "penaltyPoints",  ignore = true) // managed by domain ops
    @Mapping(target = "suspended",    ignore = true) // managed by domain ops
    @Mapping(target = "suspendedUntil", ignore = true) // managed by domain ops
    @Mapping(target = "user",           ignore = true) // managed by link-account endpoint
    @Mapping(target = "created",        ignore = true)
    @Mapping(target = "updated",        ignore = true)
    @Mapping(target = "createdBy",      ignore = true)
    @Mapping(target = "updatedBy",      ignore = true)
    @Mapping(target = "deletedAt",      ignore = true)
    void toEntityFromUpdate(DriverUpdateRequest request, @MappingTarget DriverEntity entity);

    @Override
    @Mapping(target = "userId",         source = "user.id")
    @Mapping(target = "licenseExpired", ignore = true) // set in @AfterMapping
    @Mapping(target = "dateOfBirth",    source = "dateOfBirth", dateFormat = "yyyy-MM-dd")
    DriverDto toDto(DriverEntity entity);

    /**
     * Computes the derived licenseExpired flag after the main mapping runs.
     * MapStruct calls this automatically because of the @AfterMapping annotation.
     */
    @AfterMapping
    default void computeDerivedFields(DriverEntity entity, @MappingTarget DriverDto dto) {
        if (entity.getLicenseExpiresAt() != null) {
            dto.setLicenseExpired(entity.getLicenseExpiresAt().isBefore(LocalDate.now()));
        }
    }
}
