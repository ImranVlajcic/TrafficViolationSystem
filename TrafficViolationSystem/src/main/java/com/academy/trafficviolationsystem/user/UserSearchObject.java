package com.academy.trafficviolationsystem.user;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Search/filter parameters for GET /api/users.
 *
 * All fields are optional. Null fields are ignored by the Criteria query
 * in UserService.additionalFilter().
 *
 * Example requests:
 *   GET /api/users?role=OFFICER&isActive=true
 *   GET /api/users?search=john&page=0&limit=20
 *   GET /api/users?includeCount=true
 */
@Getter
@Setter
public class UserSearchObject extends BaseSearchObject<UUID> {

    /**
     * Free-text search — matched against username, email, firstName, lastName.
     * Implemented as a case-insensitive LIKE on each field with OR logic.
     */
    private String search;

    /** Filter by exact role. */
    private UserRole role;

    /** Filter by active/inactive status. Null returns both. */
    private Boolean isActive;
}
