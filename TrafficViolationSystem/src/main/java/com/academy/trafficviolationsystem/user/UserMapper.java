package com.academy.trafficviolationsystem.user;

import com.academy.trafficviolationsystem.core.mappers.BaseCRUDMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for the user module.
 *
 * MapStruct generates the implementation at compile time — you never write
 * the mapping code manually. IntelliJ shows it under target/generated-sources.
 *
 * componentModel = "spring" → the generated class is a @Component and can
 * be @Autowired / constructor-injected anywhere.
 *
 * Key mapping decisions:
 *
 *  toEntityFromInsert:
 *    - password → ignored here; UserService.beforeInsert() hashes and sets it.
 *    - passwordHash → explicitly ignored so MapStruct doesn't try to map it.
 *
 *  toEntityFromUpdate:
 *    - NullValuePropertyMappingStrategy.IGNORE → null fields in the request
 *      leave the entity field unchanged (enables partial/PATCH-style updates).
 *    - password fields ignored for the same reason as insert.
 */
@Mapper(componentModel = "spring")
public interface UserMapper extends BaseCRUDMapper<UserEntity, UserDto, UserCreateRequest, UserUpdateRequest> {

    @Override
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "passwordHash", ignore = true)  // set by UserService after BCrypt hashing
    @Mapping(target = "active",     constant = "true")
    @Mapping(target = "failedLogins", constant = "0")
    @Mapping(target = "lastLoginAt",  ignore = true)
    @Mapping(target = "lockedUntil",  ignore = true)
    @Mapping(target = "created",      ignore = true)
    @Mapping(target = "updated",      ignore = true)
    @Mapping(target = "createdBy",    ignore = true)
    @Mapping(target = "updatedBy",    ignore = true)
    @Mapping(target = "deletedAt",    ignore = true)
    UserEntity toEntityFromInsert(UserCreateRequest request);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "username",     ignore = true)  // username cannot be changed after creation
    @Mapping(target = "passwordHash", ignore = true)  // use change-password endpoint
    @Mapping(target = "failedLogins", ignore = true)
    @Mapping(target = "lastLoginAt",  ignore = true)
    @Mapping(target = "lockedUntil",  ignore = true)
    @Mapping(target = "created",      ignore = true)
    @Mapping(target = "updated",      ignore = true)
    @Mapping(target = "createdBy",    ignore = true)
    @Mapping(target = "updatedBy",    ignore = true)
    @Mapping(target = "deletedAt",    ignore = true)
    void toEntityFromUpdate(UserUpdateRequest request, @MappingTarget UserEntity entity);

    @Override
    UserDto toDto(UserEntity entity);
}
