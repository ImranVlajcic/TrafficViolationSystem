package com.academy.trafficviolationsystem.violation;

import com.academy.trafficviolationsystem.core.mappers.BaseCRUDMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * MapStruct mapper for the violation module.
 *
 * toEntityFromInsert:
 *   - status, referenceNumber, fineId are ignored — ViolationService.beforeInsert()
 *     sets them based on business rules (status depends on detectionMethod,
 *     referenceNumber is generated, fineId starts null).
 *   - vehicle, driver, officer, reviewedBy are JPA relationships loaded
 *     by ViolationService.beforeInsert() from the raw UUID fields in the request.
 *
 * toEntityFromUpdate:
 *   - IGNORE strategy — null fields keep existing values.
 *   - Immutable fields (violationType, detectionMethod, vehicleId, occurredAt,
 *     cameraId, status, referenceNumber, fineId) are never touched by an update.
 *
 * toDto (@AfterMapping):
 *   - speedExcess is computed from measuredSpeed - speedLimit.
 *   - Vehicle/driver/officer summary fields are assembled from the JPA relationships.
 */
@Mapper(componentModel = "spring")
public interface ViolationMapper extends BaseCRUDMapper<ViolationEntity, ViolationDto, ViolationCreateRequest, ViolationUpdateRequest> {

    @Override
    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "referenceNumber", ignore = true) // generated in beforeInsert
    @Mapping(target = "status",          ignore = true) // set in beforeInsert based on detectionMethod
    @Mapping(target = "fineId",          ignore = true) // set by FineService after confirmation
    @Mapping(target = "vehicle",         ignore = true) // loaded in beforeInsert
    @Mapping(target = "driver",          ignore = true) // loaded in beforeInsert
    @Mapping(target = "officer",         ignore = true) // set in beforeInsert from principal
    @Mapping(target = "reviewedBy",      ignore = true) // set by confirm/dismiss operations
    @Mapping(target = "reviewedAt",      ignore = true) // set by confirm/dismiss operations
    @Mapping(target = "created",         ignore = true)
    @Mapping(target = "updated",         ignore = true)
    @Mapping(target = "createdBy",       ignore = true)
    @Mapping(target = "updatedBy",       ignore = true)
    @Mapping(target = "deletedAt",       ignore = true)
    ViolationEntity toEntityFromInsert(ViolationCreateRequest request);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "violationType",   ignore = true) // immutable
    @Mapping(target = "detectionMethod", ignore = true) // immutable
    @Mapping(target = "occurredAt",      ignore = true) // immutable
    @Mapping(target = "locationLatitude",ignore = true) // immutable
    @Mapping(target = "locationLongitude",ignore = true)// immutable
    @Mapping(target = "status",          ignore = true) // use confirm/dismiss endpoints
    @Mapping(target = "fineId",          ignore = true) // managed by FineService
    @Mapping(target = "cameraId",        ignore = true) // immutable
    @Mapping(target = "automatic",     ignore = true) // immutable
    @Mapping(target = "vehicle",         ignore = true) // immutable
    @Mapping(target = "officer",         ignore = true) // immutable
    @Mapping(target = "reviewedBy",      ignore = true) // managed by confirm/dismiss
    @Mapping(target = "reviewedAt",      ignore = true) // managed by confirm/dismiss
    @Mapping(target = "driver",          ignore = true) // loaded in beforeUpdate
    @Mapping(target = "created",         ignore = true)
    @Mapping(target = "updated",         ignore = true)
    @Mapping(target = "createdBy",       ignore = true)
    @Mapping(target = "updatedBy",       ignore = true)
    @Mapping(target = "deletedAt",       ignore = true)
    void toEntityFromUpdate(ViolationUpdateRequest request, @MappingTarget ViolationEntity entity);

    @Override
    @Mapping(target = "vehicleId",          source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate",source = "vehicle.licensePlate")
    @Mapping(target = "vehicleMakeModel",   ignore = true) // set in @AfterMapping
    @Mapping(target = "driverId",           source = "driver.id")
    @Mapping(target = "driverFullName",     ignore = true) // set in @AfterMapping
    @Mapping(target = "driverLicenseNumber",source = "driver.licenseNumber")
    @Mapping(target = "officerId",          source = "officer.id")
    @Mapping(target = "officerFullName",    ignore = true) // set in @AfterMapping
    @Mapping(target = "officerBadgeNumber", source = "officer.badgeNumber")
    @Mapping(target = "reviewedById",       source = "reviewedBy.id")
    @Mapping(target = "reviewedByFullName", ignore = true) // set in @AfterMapping
    @Mapping(target = "speedExcess",        ignore = true) // computed in @AfterMapping
    ViolationDto toDto(ViolationEntity entity);

    @AfterMapping
    default void computeDerivedFields(ViolationEntity entity, @MappingTarget ViolationDto dto) {
        // speedExcess
        if (entity.getMeasuredSpeed() != null && entity.getSpeedLimit() != null) {
            int excess = entity.getMeasuredSpeed() - entity.getSpeedLimit();
            dto.setSpeedExcess(excess > 0 ? excess : 0);
        }

        // vehicleMakeModel
        if (entity.getVehicle() != null) {
            dto.setVehicleMakeModel(entity.getVehicle().getMake() + " " + entity.getVehicle().getModel());
        }

        // driverFullName
        if (entity.getDriver() != null) {
            dto.setDriverFullName(entity.getDriver().getFirstName() + " " + entity.getDriver().getLastName());
        }

        // officerFullName
        if (entity.getOfficer() != null) {
            dto.setOfficerFullName(entity.getOfficer().getFirstName() + " " + entity.getOfficer().getLastName());
        }

        // reviewedByFullName
        if (entity.getReviewedBy() != null) {
            dto.setReviewedByFullName(entity.getReviewedBy().getFirstName() + " " + entity.getReviewedBy().getLastName());
        }
    }

    default LocalDateTime map(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    default Instant map(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
