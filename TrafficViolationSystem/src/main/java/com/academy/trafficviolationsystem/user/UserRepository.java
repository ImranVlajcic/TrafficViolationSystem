package com.academy.trafficviolationsystem.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for UserEntity.
 *
 * Extends JpaRepository (not CrudRepository) so we also get
 * findAll(Pageable), flush(), saveAndFlush() etc. if needed.
 *
 * The Criteria-based search in BaseService uses EntityManager directly,
 * so this repository only needs the named queries that can't be expressed
 * cleanly with Criteria (e.g. the @Modifying bulk updates).
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    // ── lookups ───────────────────────────────────────────────────────────

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByBadgeNumber(String badgeNumber);

    List<UserEntity> findByRoleInAndIsActiveTrue(List<UserRole> roles);

    // ── login / locking ───────────────────────────────────────────────────

    /**
     * Increment failed login counter atomically.
     * Called by AuthService on every failed login attempt.
     */
    @Modifying
    @Query("UPDATE UserEntity u SET u.failedLogins = u.failedLogins + 1 WHERE u.id = :id")
    void incrementFailedLogins(@Param("id") UUID id);

    /**
     * Reset counter and record successful login time.
     * Called by AuthService after a successful authentication.
     */
    @Modifying
    @Query("UPDATE UserEntity u SET u.failedLogins = 0, u.lastLoginAt = :now WHERE u.id = :id")
    void recordSuccessfulLogin(@Param("id") UUID id, @Param("now") LocalDateTime now);

    /**
     * Lock the account until the given timestamp.
     * Called by AuthService when failedLogins reaches the threshold.
     */
    @Modifying
    @Query("UPDATE UserEntity u SET u.lockedUntil = :until WHERE u.id = :id")
    void lockAccount(@Param("id") UUID id, @Param("until") LocalDateTime until);
}
