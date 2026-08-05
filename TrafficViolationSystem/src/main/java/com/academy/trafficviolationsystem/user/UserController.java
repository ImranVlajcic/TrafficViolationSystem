package com.academy.trafficviolationsystem.user;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import com.academy.trafficviolationsystem.core.services.BaseCRUDService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for user management.
 * Mapped to /api/users — secured to ADMIN only in SecurityConfig.
 *
 * Inherits from BaseCRUDController:
 *   GET    /api/users           → search(UserSearchObject) → PagedResult<UserDto>
 *   GET    /api/users/{id}      → findById(UUID)           → UserDto
 *   POST   /api/users           → create(UserCreateRequest)→ UserDto
 *   PUT    /api/users/{id}      → update(UUID, UserUpdateRequest) → UserDto
 *
 * Extra endpoints defined here:
 *   GET    /api/users/me              → current user's own profile
 *   POST   /api/users/{id}/change-password
 *   DELETE /api/users/{id}            → soft-delete
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management — ADMIN only except /me")
public class UserController implements com.academy.trafficviolationsystem.core.controllers.BaseCRUDController<
        UserEntity, UserDto, UserSearchObject, UserCreateRequest, UserUpdateRequest, UUID> {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserService getService() {
        return userService;
    }

    // ── /me — any authenticated user ──────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(@CurrentUser UserPrincipal principal) {
        UserDto dto = userService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    // ── change password ───────────────────────────────────────────────────

    @PostMapping("/{id}/change-password")
    @Operation(summary = "Change a user's password")
    @PreAuthorize("hasRole('ADMIN') or #principal.id == #id")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request,
            @CurrentUser UserPrincipal principal) {

        boolean isAdmin = principal.isAdmin();
        userService.changePassword(id, request, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }

    // ── soft-delete ───────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a user account (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        UserEntity user = userService.findEntityById(id);
        userService.getRepository().delete(user); // triggers @PreRemove → sets deletedAt
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok("User deactivated successfully", null));
    }
}
