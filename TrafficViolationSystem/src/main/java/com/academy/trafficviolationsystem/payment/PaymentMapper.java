package com.academy.trafficviolationsystem.payment;

import com.academy.trafficviolationsystem.core.mappers.BaseMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for the payment module.
 *
 * PaymentEntity is never created from a request body — PaymentService builds
 * it programmatically after the gateway simulation. So only toDto() is needed,
 * not the full BaseCRUDMapper.
 *
 * toDto mappings:
 *   fineId       ← fine.id
 *   paidById     ← paidBy.id
 *   paidByUsername ← paidBy.username
 *   receiptReady   ignored here → set in @AfterMapping
 *   fineNumber     ignored here → populated by PaymentService.toDtoWithFineNumber()
 *
 * @AfterMapping:
 *   receiptReady = receiptPdfPath != null
 */
@Mapper(componentModel = "spring")
public interface PaymentMapper extends BaseMapper<PaymentEntity, PaymentDto> {

    @Override
    @Mapping(target = "fineId",        source = "fine.id")
    @Mapping(target = "paidById",      source = "paidBy.id")
    @Mapping(target = "paidByUsername",source = "paidBy.username")
    @Mapping(target = "receiptReady",  ignore = true) // set in @AfterMapping
    @Mapping(target = "fineNumber",    ignore = true) // set by PaymentService
    PaymentDto toDto(PaymentEntity entity);

    @AfterMapping
    default void computeDerived(PaymentEntity entity, @MappingTarget PaymentDto dto) {
        dto.setReceiptReady(entity.getReceiptPdfPath() != null);
    }
}
