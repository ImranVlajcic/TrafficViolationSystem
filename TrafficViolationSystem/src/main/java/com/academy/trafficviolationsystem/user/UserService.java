package com.academy.trafficviolationsystem.user;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.auth.DuplicateResourceException;
import com.academy.trafficviolationsystem.core.exceptions.auth.UnauthorizedException;
import com.academy.trafficviolationsystem.core.services.BaseCRUDService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for user management.
 *
 * Implements BaseCRUDService, so insert() / update() / search() / findById()
 * are all provided by the base interfaces. Only the hooks and domain-specific
 * methods need to be written here.
 *
 * Lifecycle hooks wired in:
 *   beforeInsert → duplicate checks, badge validation, password hashing
 *   beforeUpdate → duplicate checks, role-change guards
 *   afterUpdate  → revoke refresh tokens if account disabled
 *
 * Extra endpoints beyond CRUD:
 *   changePassword(id, request, principal) → POST /api/users/{id}/change-password
 *   getProfile(principal)                  → GET /api/users/me
 */
@Service
@Transactional
public class UserService implements BaseCRUDService<
        UserEntity, UserDto, UserSearchObject, UserCreateRequest, UserUpdateRequest, UUID> {

    // ── constants ─────────────────────────────────────────────────────────

    private static final int MAX_FAILED_LOGINS    = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    // ── dependencies ──────────────────────────────────────────────────────

    private final UserRepository        userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper            userMapper;
    private final PasswordEncoder       passwordEncoder;
    private final EntityManager         entityManager;

    public UserService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       EntityManager entityManager) {
        this.userRepository        = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper            = userMapper;
        this.passwordEncoder       = passwordEncoder;
        this.entityManager         = entityManager;
    }

    // ── BaseCRUDService wiring ────────────────────────────────────────────

    @Override public UserRepository getRepository()    { return userRepository; }
    @Override public EntityManager  getEntityManager() { return entityManager;  }
    @Override public UserMapper     getMapper()        { return userMapper;     }
    @Override public Class<UserEntity> getEntityClass(){ return UserEntity.class; }

    // ── lifecycle hooks ───────────────────────────────────────────────────

    @Override
    @AuditAction(value = "CREATE_USER", entityClass = UserEntity.class)
    public UserDto insert(UserCreateRequest request) {
        return BaseCRUDService.super.insert(request);
    }

    @Override
    @AuditAction(value = "UPDATE_USER", entityClass = UserEntity.class)
    public UserDto update(UUID id, UserUpdateRequest request) {
        return BaseCRUDService.super.update(id, request);
    }

    @Override
    public void beforeInsert(UserCreateRequest request, UserEntity entity) {
        // Uniqueness guards
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        // Officers must have a badge number
        if (request.getRole() == UserRole.OFFICER) {
            if (request.getBadgeNumber() == null || request.getBadgeNumber().isBlank()) {
                throw new BadRequestException("Badge number is required for OFFICER accounts");
            }
            if (userRepository.existsByBadgeNumber(request.getBadgeNumber())) {
                throw new DuplicateResourceException("Badge number '" + request.getBadgeNumber() + "' is already assigned");
            }
        }

        // Hash the password before persisting — the mapper leaves passwordHash null
        entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    }

    @Override
    public void beforeUpdate(UserUpdateRequest request, UserEntity entity) {
        // Email uniqueness check (only if changing)
        if (request.getEmail() != null && !request.getEmail().equals(entity.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
            }
        }

        // Badge number uniqueness check (only if changing)
        if (request.getBadgeNumber() != null && !request.getBadgeNumber().equals(entity.getBadgeNumber())) {
            if (userRepository.existsByBadgeNumber(request.getBadgeNumber())) {
                throw new BadRequestException("Badge number '" + request.getBadgeNumber() + "' is already assigned");
            }
        }

        // If promoting to OFFICER, badge is now required
        UserRole targetRole = request.getRole() != null ? request.getRole() : entity.getRole();
        String targetBadge  = request.getBadgeNumber() != null ? request.getBadgeNumber() : entity.getBadgeNumber();
        if (targetRole == UserRole.OFFICER && (targetBadge == null || targetBadge.isBlank())) {
            throw new BadRequestException("Badge number is required for OFFICER accounts");
        }
    }

    @Override
    public void afterUpdate(UserUpdateRequest request, UserEntity entity) {
        // If account just got disabled, immediately invalidate all sessions
        if (Boolean.FALSE.equals(request.getIsActive())) {
            refreshTokenRepository.revokeAllForUser(entity.getId());
        }
    }

    // ── search filters ────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                            UserSearchObject searchObj,
                                            Root<UserEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        // Free-text search across username, email, firstName, lastName
        if (searchObj.getSearch() != null && !searchObj.getSearch().isBlank()) {
            String pattern = "%" + searchObj.getSearch().toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("username")),  pattern),
                cb.like(cb.lower(root.get("email")),     pattern),
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")),  pattern)
            ));
        }

        if (searchObj.getRole() != null) {
            predicates.add(cb.equal(root.get("role"), searchObj.getRole()));
        }

        if (searchObj.getIsActive() != null) {
            predicates.add(cb.equal(root.get("isActive"), searchObj.getIsActive()));
        }

        return predicates;
    }

    // ── extra domain operations ───────────────────────────────────────────

    /**
     * Change a user's password.
     * - Regular users must provide their current password for confirmation.
     * - Admins can reset any user's password without the current password.
     *
     * All active refresh tokens are revoked after a password change so
     * any stolen sessions are immediately invalidated.
     */
    @Transactional
    @AuditAction(value = "CHANGE_PASSWORD", entityClass = UserEntity.class, captureSnapshot = false)
    public void changePassword(UUID userId, ChangePasswordRequest request, boolean isAdmin) {
        UserEntity user = findEntityById(userId);

        if (!isAdmin) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new UnauthorizedException("Current password is incorrect");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all sessions so other devices must re-authenticate
        refreshTokenRepository.revokeAllForUser(userId);
    }

    /**
     * Returns the UserDto for the currently authenticated user.
     * Used by GET /api/users/me.
     */
    @Transactional(readOnly = true)
    public UserDto getProfile(UUID userId) {
        return findById(userId);
    }
}
