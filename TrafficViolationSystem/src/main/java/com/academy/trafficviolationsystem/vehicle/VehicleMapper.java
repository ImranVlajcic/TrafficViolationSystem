package com.academy.trafficviolationsystem.vehicle;

import com.academy.trafficviolationsystem.core.mappers.BaseCRUDMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDate;

/**
 * MapStruct mapper for the vehicle module.
 *
 * Notable decisions:
 *
 *  toEntityFromInsert:
 *    - owner is ignored — VehicleService.beforeInsert() loads and sets it
 *      from ownerId using DriverRepository, so MapStruct never touches
 *      the relationship object directly.
 *    - isStolen defaults to false, isActive to true.
 *
 *  toEntityFromUpdate:
 *    - IGNORE strategy — null fields leave the entity unchanged.
 *    - licensePlate, vin, owner are immutable via this path.
 *
 *  toDto (@AfterMapping):
 *    - registrationExpired is computed from registrationExpiry vs today.
 *    - ownerFullName is assembled from owner.firstName + owner.lastName.
 *    - ownerLicenseNumber is taken from owner.licenseNumber.
 */
@Mapper(componentModel = "spring")
public interface VehicleMapper extends BaseCRUDMapper<VehicleEntity, VehicleDto, VehicleCreateRequest, VehicleUpdateRequest> {

    @Override
    @Mapping(target = "id",                   ignore = true)
    @Mapping(target = "owner",                ignore = true)  // set by VehicleService.beforeInsert()
    @Mapping(target = "stolen",             constant = "false")
    @Mapping(target = "active",             constant = "true")
    @Mapping(target = "created",              ignore = true)
    @Mapping(target = "updated",              ignore = true)
    @Mapping(target = "createdBy",            ignore = true)
    @Mapping(target = "updatedBy",            ignore = true)
    @Mapping(target = "deletedAt",            ignore = true)
    VehicleEntity toEntityFromInsert(VehicleCreateRequest request);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",                   ignore = true)
    @Mapping(target = "licensePlate",         ignore = true)  // immutable after registration
    @Mapping(target = "vin",                  ignore = true)  // immutable after registration
    @Mapping(target = "owner",                ignore = true)  // use transfer-ownership endpoint
    @Mapping(target = "stolen",             ignore = true)  // use mark-stolen / mark-found
    @Mapping(target = "created",              ignore = true)
    @Mapping(target = "updated",              ignore = true)
    @Mapping(target = "createdBy",            ignore = true)
    @Mapping(target = "updatedBy",            ignore = true)
    @Mapping(target = "deletedAt",            ignore = true)
    void toEntityFromUpdate(VehicleUpdateRequest request, @MappingTarget VehicleEntity entity);

    @Override
    @Mapping(target = "ownerId",              source = "owner.id")
    @Mapping(target = "ownerFullName",        ignore = true)  // set in @AfterMapping
    @Mapping(target = "ownerLicenseNumber",   source = "owner.licenseNumber")
    @Mapping(target = "registrationExpired",  ignore = true)  // set in @AfterMapping
    VehicleDto toDto(VehicleEntity entity);

    @AfterMapping
    default void computeDerivedFields(VehicleEntity entity, @MappingTarget VehicleDto dto) {
        // registrationExpired
        if (entity.getRegistrationExpiry() != null) {
            dto.setRegistrationExpired(entity.getRegistrationExpiry().isBefore(LocalDate.now()));
        }

        // ownerFullName assembled from the JPA relationship
        if (entity.getOwner() != null) {
            dto.setOwnerFullName(
                entity.getOwner().getFirstName() + " " + entity.getOwner().getLastName()
            );
        }
    }
}
