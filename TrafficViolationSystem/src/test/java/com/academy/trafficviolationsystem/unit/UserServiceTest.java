package com.academy.trafficviolationsystem.unit;

import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.auth.DuplicateResourceException;
import com.academy.trafficviolationsystem.core.exceptions.auth.UnauthorizedException;
import com.academy.trafficviolationsystem.user.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 *
 * These target the logic UserService actually owns: the beforeInsert /
 * beforeUpdate / afterUpdate hooks, changePassword, and additionalFilter.
 * insert()/update() themselves are default methods on BaseCRUDService and
 * are exercised indirectly here (through the hooks), not re-tested —
 * a separate test on BaseCRUDServiceTest (if you write one generically)
 * would cover the save/mapper plumbing itself.
 *
 * All collaborators are mocked — no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private UserService userService;

    private UserEntity citizenEntity;
    private UserEntity officerEntity;

    @BeforeEach
    void setUp() {
        citizenEntity = new UserEntity();
        citizenEntity.setId(UUID.randomUUID());
        citizenEntity.setUsername("citizen1");
        citizenEntity.setEmail("citizen1@example.com");
        citizenEntity.setPasswordHash("old-hash");
        citizenEntity.setRole(UserRole.CITIZEN);
        citizenEntity.setBadgeNumber(null);

        officerEntity = new UserEntity();
        officerEntity.setId(UUID.randomUUID());
        officerEntity.setUsername("officer1");
        officerEntity.setEmail("officer1@example.com");
        officerEntity.setPasswordHash("old-hash");
        officerEntity.setRole(UserRole.OFFICER);
        officerEntity.setBadgeNumber("B-100");
    }

    // ───────────────────────────── beforeInsert ─────────────────────────────

    @Nested
    class BeforeInsertTests {

        @Test
        void throwsWhenUsernameTaken() {
            UserCreateRequest req = new UserCreateRequest();
            req.setUsername("taken");
            req.setEmail("new@example.com");
            req.setRole(UserRole.CITIZEN);

            when(userRepository.existsByUsername("taken")).thenReturn(true);

            assertThatThrownBy(() -> userService.beforeInsert(req, new UserEntity()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("taken");

            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        void throwsWhenEmailTaken() {
            UserCreateRequest req = new UserCreateRequest();
            req.setUsername("newuser");
            req.setEmail("taken@example.com");
            req.setRole(UserRole.CITIZEN);

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.beforeInsert(req, new UserEntity()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("registered");
        }

        @Test
        void throwsWhenOfficerHasNoBadgeNumber() {
            UserCreateRequest req = new UserCreateRequest();
            req.setUsername("newofficer");
            req.setEmail("newofficer@example.com");
            req.setRole(UserRole.OFFICER);
            req.setBadgeNumber(null);

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            assertThatThrownBy(() -> userService.beforeInsert(req, new UserEntity()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Badge number is required");
        }

        @Test
        void throwsWhenOfficerBadgeAlreadyAssigned() {
            UserCreateRequest req = new UserCreateRequest();
            req.setUsername("newofficer");
            req.setEmail("newofficer@example.com");
            req.setRole(UserRole.OFFICER);
            req.setBadgeNumber("B-100");

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByBadgeNumber("B-100")).thenReturn(true);

            assertThatThrownBy(() -> userService.beforeInsert(req, new UserEntity()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("already assigned");
        }

        @Test
        void hashesPasswordAndSetsOnEntityForValidRequest() {
            UserCreateRequest req = new UserCreateRequest();
            req.setUsername("newcitizen");
            req.setEmail("newcitizen@example.com");
            req.setPassword("PlainText1!");
            req.setRole(UserRole.CITIZEN);

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("PlainText1!")).thenReturn("hashed-value");

            UserEntity entity = new UserEntity();
            userService.beforeInsert(req, entity);

            assertThat(entity.getPasswordHash()).isEqualTo("hashed-value");
            // CITIZEN role, so badge uniqueness must never be checked
            verify(userRepository, never()).existsByBadgeNumber(anyString());
        }
    }

    // ───────────────────────────── beforeUpdate ─────────────────────────────

    @Nested
    class BeforeUpdateTests {

        @Test
        void allowsUpdateWhenNothingChanges() {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setFirstName("NewFirstName");

            userService.beforeUpdate(req, officerEntity);

            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).existsByBadgeNumber(anyString());
        }

        @Test
        void throwsWhenNewEmailAlreadyRegistered() {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setEmail("someoneelse@example.com");

            when(userRepository.existsByEmail("someoneelse@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.beforeUpdate(req, citizenEntity))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("registered");
        }

        @Test
        void doesNotCheckEmailWhenUnchanged() {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setEmail(citizenEntity.getEmail()); // same value → not "changing"

            userService.beforeUpdate(req, citizenEntity);

            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        void throwsWhenNewBadgeNumberAlreadyAssigned() {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setBadgeNumber("B-999");

            when(userRepository.existsByBadgeNumber("B-999")).thenReturn(true);

            assertThatThrownBy(() -> userService.beforeUpdate(req, officerEntity))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already assigned");
        }

        @Test
        void throwsWhenPromotingToOfficerWithoutBadge() {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setRole(UserRole.OFFICER);
            // no badge supplied, and citizenEntity has none → should reject

            assertThatThrownBy(() -> userService.beforeUpdate(req, citizenEntity))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Badge number is required");
        }

        @Test
        void allowsPromotionToOfficerWhenBadgeProvided() {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setRole(UserRole.OFFICER);
            req.setBadgeNumber("B-555");

            when(userRepository.existsByBadgeNumber("B-555")).thenReturn(false);

            userService.beforeUpdate(req, citizenEntity); // should not throw
        }

        @Test
        void allowsUpdateForExistingOfficerKeepingSameBadge() {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setFirstName("Changed");
            // role/badge left null → falls back to entity's existing OFFICER + "B-100",
            // which is already valid, so no uniqueness check should fire

            userService.beforeUpdate(req, officerEntity);

            verify(userRepository, never()).existsByBadgeNumber(anyString());
        }
    }

    // ───────────────────────────── afterUpdate ─────────────────────────────

    @Nested
    class AfterUpdateTests {

        @Test
        void revokesTokensWhenAccountDisabled() {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setIsActive(false);

            userService.afterUpdate(req, citizenEntity);

            verify(refreshTokenRepository).revokeAllForUser(citizenEntity.getId());
        }

        @Test
        void doesNotRevokeTokensWhenAccountStaysActive() {
            UserUpdateRequest req = new UserUpdateRequest();
            req.setIsActive(true);

            userService.afterUpdate(req, citizenEntity);

            verify(refreshTokenRepository, never()).revokeAllForUser(any());
        }

        @Test
        void doesNotRevokeTokensWhenIsActiveNotProvided() {
            UserUpdateRequest req = new UserUpdateRequest();
            // null == "keep existing value", not an explicit disable

            userService.afterUpdate(req, citizenEntity);

            verify(refreshTokenRepository, never()).revokeAllForUser(any());
        }
    }

    // ───────────────────────────── changePassword ─────────────────────────────
    // NOTE: relies on findEntityById(), inherited from BaseService, assumed
    // here to resolve via userRepository.findById(id). Share BaseService.java
    // if it works differently (e.g. via EntityManager.find) and I'll adjust.

    @Nested
    class ChangePasswordTests {

        @Test
        void changesPasswordWhenCurrentPasswordMatches() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("oldPass1!");
            req.setNewPassword("newPass1!");

            when(userRepository.findById(citizenEntity.getId())).thenReturn(Optional.of(citizenEntity));
            when(passwordEncoder.matches("oldPass1!", citizenEntity.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("newPass1!")).thenReturn("new-hash");

            userService.changePassword(citizenEntity.getId(), req, false);

            assertThat(citizenEntity.getPasswordHash()).isEqualTo("new-hash");
            verify(userRepository).save(citizenEntity);
            verify(refreshTokenRepository).revokeAllForUser(citizenEntity.getId());
        }

        @Test
        void throwsWhenCurrentPasswordIsWrong() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("wrongPass");
            req.setNewPassword("newPass1!");

            when(userRepository.findById(citizenEntity.getId())).thenReturn(Optional.of(citizenEntity));
            when(passwordEncoder.matches("wrongPass", citizenEntity.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(citizenEntity.getId(), req, false))
                    .isInstanceOf(UnauthorizedException.class);

            verify(userRepository, never()).save(any());
            verify(refreshTokenRepository, never()).revokeAllForUser(any());
        }

        @Test
        void adminBypassesCurrentPasswordCheck() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setNewPassword("adminSetPass1!");
            // currentPassword deliberately left unset — admin path must not need it

            when(userRepository.findById(officerEntity.getId())).thenReturn(Optional.of(officerEntity));
            when(passwordEncoder.encode("adminSetPass1!")).thenReturn("admin-hash");

            userService.changePassword(officerEntity.getId(), req, true);

            assertThat(officerEntity.getPasswordHash()).isEqualTo("admin-hash");
            verify(passwordEncoder, never()).matches(any(), any());
            verify(refreshTokenRepository).revokeAllForUser(officerEntity.getId());
        }
    }

    // ───────────────────────────── additionalFilter ─────────────────────────────
    // Pure logic test against the mocked Criteria API — no DB, no Spring context.

    @Nested
    class AdditionalFilterTests {

        @Test
        @SuppressWarnings("unchecked")
        void buildsFreeTextSearchPredicateAcrossFourFields() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<UserEntity> root = mock(Root.class);
            Path<Object> path = mock(Path.class);
            Expression<String> lowered = mock(Expression.class);
            Predicate likePredicate = mock(Predicate.class);
            Predicate orPredicate = mock(Predicate.class);

            when(root.<Object>get(anyString())).thenReturn(path);
            when(cb.lower(any())).thenReturn(lowered);
            when(cb.like(any(), anyString())).thenReturn(likePredicate);
            when(cb.or(any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class)))
                    .thenReturn(orPredicate);

            UserSearchObject searchObj = new UserSearchObject();
            searchObj.setSearch("john");

            List<Predicate> predicates = userService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactly(orPredicate);
            // username, email, firstName, lastName → 4 LIKE clauses, same pattern
            verify(cb, times(4)).like(any(), eq("%john%"));
        }

        @Test
        @SuppressWarnings("unchecked")
        void addsRoleAndActiveFiltersWhenPresent() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<UserEntity> root = mock(Root.class);
            Path<Object> rolePath = mock(Path.class);
            Path<Object> activePath = mock(Path.class);
            Predicate rolePredicate = mock(Predicate.class);
            Predicate activePredicate = mock(Predicate.class);

            when(root.<Object>get("role")).thenReturn(rolePath);
            when(root.<Object>get("isActive")).thenReturn(activePath);
            when(cb.equal(rolePath, UserRole.OFFICER)).thenReturn(rolePredicate);
            when(cb.equal(activePath, true)).thenReturn(activePredicate);

            UserSearchObject searchObj = new UserSearchObject();
            searchObj.setRole(UserRole.OFFICER);
            searchObj.setIsActive(true);

            List<Predicate> predicates = userService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactlyInAnyOrder(rolePredicate, activePredicate);
        }

        @Test
        void returnsEmptyListWhenNoFiltersProvided() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<UserEntity> root = mock(Root.class);
            UserSearchObject searchObj = new UserSearchObject();

            List<Predicate> predicates = userService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).isEmpty();
        }
    }
}
