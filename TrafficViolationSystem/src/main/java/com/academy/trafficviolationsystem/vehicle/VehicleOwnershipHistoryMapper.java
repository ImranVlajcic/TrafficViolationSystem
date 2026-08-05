package com.academy.trafficviolationsystem.vehicle;

import com.academy.trafficviolationsystem.core.mappers.BaseMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Maps VehicleOwnershipHistoryEntity → VehicleOwnershipHistoryDto.
 * Read-only — rows are written directly by VehicleService.transferOwnership().
 */
@Mapper(componentModel = "spring")
public interface VehicleOwnershipHistoryMapper extends BaseMapper<VehicleOwnershipHistoryEntity, VehicleOwnershipHistoryDto> {

    @Override
    @Mapping(target = "previousOwnerId",            source = "previousOwner.id")
    @Mapping(target = "previousOwnerLicenseNumber", source = "previousOwner.licenseNumber")
    @Mapping(target = "previousOwnerFullName",      ignore = true) // set in @AfterMapping
    @Mapping(target = "newOwnerId",                 source = "newOwner.id")
    @Mapping(target = "newOwnerLicenseNumber",      source = "newOwner.licenseNumber")
    @Mapping(target = "newOwnerFullName",            ignore = true) // set in @AfterMapping
    VehicleOwnershipHistoryDto toDto(VehicleOwnershipHistoryEntity entity);

    @AfterMapping
    default void assembleNames(VehicleOwnershipHistoryEntity entity,
                                @MappingTarget VehicleOwnershipHistoryDto dto) {
        if (entity.getPreviousOwner() != null) {
            dto.setPreviousOwnerFullName(
                entity.getPreviousOwner().getFirstName() + " " + entity.getPreviousOwner().getLastName()
            );
        }
        if (entity.getNewOwner() != null) {
            dto.setNewOwnerFullName(
                entity.getNewOwner().getFirstName() + " " + entity.getNewOwner().getLastName()
            );
        }
    }
}
