package com.academy.trafficviolationsystem.unit;

import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.driver.DriverAlreadyLinkedException;
import com.academy.trafficviolationsystem.core.exceptions.driver.DriverAlreadySuspendedException;
import com.academy.trafficviolationsystem.core.exceptions.driver.DriverNotSuspendedException;
import com.academy.trafficviolationsystem.core.exceptions.driver.UserAlreadyLinkedToDriverException;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import com.academy.trafficviolationsystem.driver.*;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DriverService.
 *
 * These target the logic DriverService actually owns: beforeInsert /
 * beforeUpdate hooks, additionalFilter, penalty-point accounting
 * (applyPenaltyPoints / removePenaltyPoints), suspension management
 * (suspend / liftSuspension), and the citizen account link. insert()/
 * update() themselves are default methods on BaseCRUDService and are
 * exercised indirectly here (through the hooks), not re-tested.
 *
 * All collaborators are mocked — no Spring context, no database.
 *
 * ASSUMPTION (same as UserServiceTest): findEntityById(), inherited from
 * BaseService, is assumed to resolve via driverRepository.findById(id).
 * Share BaseService.java if it works differently and I'll adjust.
 *
 * ASSUMPTION: DriverCreateRequest/DriverUpdateRequest/DriverSearchObject/
 * SuspendDriverRequest are plain Lombok @Getter/@Setter DTOs with the
 * field names used inside DriverService (licenseNumber, nationalId,
 * licenseIssuedAt, licenseExpiresAt, reason, endDate, violationId,
 * search, isSuspended, licenseCategory, licenseExpired). If any of
 * these differ, send the DTO/model files and I'll adjust the tests.
 */
@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock private DriverRepository driverRepository;
    @Mock private LicenseSuspensionRepository suspensionRepository;
    @Mock private DriverPointHistoryRepository pointHistoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private DriverMapper driverMapper;
    @Mock private LicenseSuspensionMapper suspensionMapper;
    @Mock private DriverPointHistoryMapper pointHistoryMapper;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private DriverService driverService;

    private DriverEntity activeDriver;
    private DriverEntity suspendedDriver;
    private UUID driverId;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();

        activeDriver = new DriverEntity();
        activeDriver.setId(driverId);
        activeDriver.setLicenseNumber("LIC-001");
        activeDriver.setNationalId("NID-001");
        activeDriver.setFirstName("Amar");
        activeDriver.setLastName("Kovac");
        activeDriver.setLicenseIssuedAt(LocalDate.of(2020, 1, 1));
        activeDriver.setLicenseExpiresAt(LocalDate.of(2030, 1, 1));
        activeDriver.setPenaltyPoints(0);
        activeDriver.setSuspended(false);

        suspendedDriver = new DriverEntity();
        suspendedDriver.setId(UUID.randomUUID());
        suspendedDriver.setLicenseNumber("LIC-002");
        suspendedDriver.setNationalId("NID-002");
        suspendedDriver.setFirstName("Selma");
        suspendedDriver.setLastName("Hodzic");
        suspendedDriver.setLicenseIssuedAt(LocalDate.of(2019, 1, 1));
        suspendedDriver.setLicenseExpiresAt(LocalDate.of(2029, 1, 1));
        suspendedDriver.setPenaltyPoints(12);
        suspendedDriver.setSuspended(true);
        suspendedDriver.setSuspendedUntil(LocalDate.now().plusDays(30));
    }

    // ───────────────────────────── beforeInsert ─────────────────────────────

    @Nested
    class BeforeInsertTests {

        @Test
        void throwsWhenLicenseNumberTaken() {
            DriverCreateRequest req = new DriverCreateRequest();
            req.setLicenseNumber("LIC-999");
            req.setNationalId("NID-999");
            req.setLicenseIssuedAt(LocalDate.of(2020, 1, 1));
            req.setLicenseExpiresAt(LocalDate.of(2030, 1, 1));

            when(driverRepository.existsByLicenseNumber("LIC-999")).thenReturn(true);

            assertThatThrownBy(() -> driverService.beforeInsert(req, new DriverEntity()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("LIC-999");

            verify(driverRepository, never()).existsByNationalId(anyString());
        }

        @Test
        void throwsWhenNationalIdTaken() {
            DriverCreateRequest req = new DriverCreateRequest();
            req.setLicenseNumber("LIC-NEW");
            req.setNationalId("NID-TAKEN");
            req.setLicenseIssuedAt(LocalDate.of(2020, 1, 1));
            req.setLicenseExpiresAt(LocalDate.of(2030, 1, 1));

            when(driverRepository.existsByLicenseNumber("LIC-NEW")).thenReturn(false);
            when(driverRepository.existsByNationalId("NID-TAKEN")).thenReturn(true);

            assertThatThrownBy(() -> driverService.beforeInsert(req, new DriverEntity()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("NID-TAKEN");
        }

        @Test
        void throwsWhenExpiryBeforeIssued() {
            DriverCreateRequest req = new DriverCreateRequest();
            req.setLicenseNumber("LIC-NEW");
            req.setNationalId("NID-NEW");
            req.setLicenseIssuedAt(LocalDate.of(2025, 1, 1));
            req.setLicenseExpiresAt(LocalDate.of(2024, 1, 1));

            when(driverRepository.existsByLicenseNumber(anyString())).thenReturn(false);
            when(driverRepository.existsByNationalId(anyString())).thenReturn(false);

            assertThatThrownBy(() -> driverService.beforeInsert(req, new DriverEntity()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expiry date");
        }

        @Test
        void allowsInsertForValidRequest() {
            DriverCreateRequest req = new DriverCreateRequest();
            req.setLicenseNumber("LIC-NEW");
            req.setNationalId("NID-NEW");
            req.setLicenseIssuedAt(LocalDate.of(2020, 1, 1));
            req.setLicenseExpiresAt(LocalDate.of(2030, 1, 1));

            when(driverRepository.existsByLicenseNumber(anyString())).thenReturn(false);
            when(driverRepository.existsByNationalId(anyString())).thenReturn(false);

            driverService.beforeInsert(req, new DriverEntity()); // should not throw
        }
    }

    // ───────────────────────────── beforeUpdate ─────────────────────────────

    @Nested
    class BeforeUpdateTests {

        @Test
        void allowsUpdateWhenNoDatesProvided() {
            DriverUpdateRequest req = new DriverUpdateRequest();
            req.setPhoneNumber("061-000-000");

            driverService.beforeUpdate(req, activeDriver); // should not throw
        }

        @Test
        void throwsWhenBothDatesProvidedAndExpiryBeforeIssued() {
            DriverUpdateRequest req = new DriverUpdateRequest();
            req.setLicenseIssuedAt(LocalDate.of(2025, 6, 1));
            req.setLicenseExpiresAt(LocalDate.of(2025, 1, 1));

            assertThatThrownBy(() -> driverService.beforeUpdate(req, activeDriver))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expiry date");
        }

        @Test
        void allowsUpdateWhenBothDatesProvidedAndValid() {
            DriverUpdateRequest req = new DriverUpdateRequest();
            req.setLicenseIssuedAt(LocalDate.of(2025, 1, 1));
            req.setLicenseExpiresAt(LocalDate.of(2030, 1, 1));

            driverService.beforeUpdate(req, activeDriver); // should not throw
        }

        @Test
        void throwsWhenOnlyExpiryProvidedAndBeforePersistedIssuedDate() {
            // activeDriver.licenseIssuedAt = 2020-01-01
            DriverUpdateRequest req = new DriverUpdateRequest();
            req.setLicenseExpiresAt(LocalDate.of(2019, 1, 1));

            assertThatThrownBy(() -> driverService.beforeUpdate(req, activeDriver))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expiry date");
        }

        @Test
        void allowsUpdateWhenOnlyExpiryProvidedAndAfterPersistedIssuedDate() {
            DriverUpdateRequest req = new DriverUpdateRequest();
            req.setLicenseExpiresAt(LocalDate.of(2031, 1, 1));

            driverService.beforeUpdate(req, activeDriver); // should not throw
        }

        @Test
        void allowsUpdateWhenOnlyIssuedDateProvided() {
            // only licenseIssuedAt set, licenseExpiresAt null → validation branch skipped entirely
            DriverUpdateRequest req = new DriverUpdateRequest();
            req.setLicenseIssuedAt(LocalDate.of(2021, 1, 1));

            driverService.beforeUpdate(req, activeDriver); // should not throw
        }
    }

    // ───────────────────────────── additionalFilter ─────────────────────────────

    @Nested
    class AdditionalFilterTests {

        @Test
        @SuppressWarnings("unchecked")
        void buildsFreeTextSearchPredicateAcrossFiveFields() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<DriverEntity> root = mock(Root.class);
            Path<Object> path = mock(Path.class);
            Expression<String> lowered = mock(Expression.class);
            Predicate likePredicate = mock(Predicate.class);
            Predicate orPredicate = mock(Predicate.class);

            when(root.<Object>get(anyString())).thenReturn(path);
            when(cb.lower(any())).thenReturn(lowered);
            when(cb.like(any(), anyString())).thenReturn(likePredicate);
            when(cb.or(any(Predicate.class), any(Predicate.class), any(Predicate.class),
                    any(Predicate.class), any(Predicate.class))).thenReturn(orPredicate);

            DriverSearchObject searchObj = new DriverSearchObject();
            searchObj.setSearch("kovac");

            List<Predicate> predicates = driverService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactly(orPredicate);
            // firstName, lastName, licenseNumber, nationalId, email → 5 LIKE clauses, same pattern
            verify(cb, times(5)).like(any(), eq("%kovac%"));
        }

        @Test
        @SuppressWarnings("unchecked")
        void addsIsSuspendedFilterWhenPresent() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<DriverEntity> root = mock(Root.class);
            Path<Object> path = mock(Path.class);
            Predicate suspendedPredicate = mock(Predicate.class);

            when(root.<Object>get("isSuspended")).thenReturn(path);
            when(cb.equal(path, true)).thenReturn(suspendedPredicate);

            DriverSearchObject searchObj = new DriverSearchObject();
            searchObj.setIsSuspended(true);

            List<Predicate> predicates = driverService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactly(suspendedPredicate);
        }

        @Test
        @SuppressWarnings("unchecked")
        void addsLicenseCategoryFilterCaseInsensitively() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<DriverEntity> root = mock(Root.class);
            Path<String> path = mock(Path.class);
            Expression<String> lowered = mock(Expression.class);
            Predicate categoryPredicate = mock(Predicate.class);

            when(root.<String>get("licenseCategory")).thenReturn(path);
            when(cb.lower(path)).thenReturn(lowered);
            when(cb.equal(lowered, "b")).thenReturn(categoryPredicate);

            DriverSearchObject searchObj = new DriverSearchObject();
            searchObj.setLicenseCategory("B");

            List<Predicate> predicates = driverService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactly(categoryPredicate);
        }

        @Test
        @SuppressWarnings("unchecked")
        void addsExpiredFilterWhenLicenseExpiredTrue() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<DriverEntity> root = mock(Root.class);
            Path<LocalDate> path = mock(Path.class);
            Predicate expiredPredicate = mock(Predicate.class);

            when(root.<LocalDate>get("licenseExpiresAt")).thenReturn(path);
            when(cb.lessThan(eq(path), any(LocalDate.class))).thenReturn(expiredPredicate);

            DriverSearchObject searchObj = new DriverSearchObject();
            searchObj.setLicenseExpired(true);

            List<Predicate> predicates = driverService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactly(expiredPredicate);
            verify(cb, never()).greaterThanOrEqualTo(any(Expression.class), any(LocalDate.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void addsNotExpiredFilterWhenLicenseExpiredFalse() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<DriverEntity> root = mock(Root.class);
            Path<LocalDate> path = mock(Path.class);
            Predicate notExpiredPredicate = mock(Predicate.class);

            when(root.<LocalDate>get("licenseExpiresAt")).thenReturn(path);
            when(cb.greaterThanOrEqualTo(eq(path), any(LocalDate.class))).thenReturn(notExpiredPredicate);

            DriverSearchObject searchObj = new DriverSearchObject();
            searchObj.setLicenseExpired(false);

            List<Predicate> predicates = driverService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactly(notExpiredPredicate);
            verify(cb, never()).lessThan(any(Expression.class), any(LocalDate.class));
        }

        @Test
        void returnsEmptyListWhenNoFiltersProvided() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<DriverEntity> root = mock(Root.class);
            DriverSearchObject searchObj = new DriverSearchObject();

            List<Predicate> predicates = driverService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).isEmpty();
        }
    }

    // ───────────────────────────── applyPenaltyPoints ─────────────────────────────

    @Nested
    class ApplyPenaltyPointsTests {

        @Test
        void addsPointsWithoutSuspensionWhenBelowThreshold() {
            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));

            DriverService.PenaltyPointsResult result =
                    driverService.applyPenaltyPoints(driverId, 5, "SPEEDING", UUID.randomUUID());

            assertThat(result.pointsBefore()).isEqualTo(0);
            assertThat(result.pointsAfter()).isEqualTo(5);
            assertThat(result.suspensionCreated()).isFalse();
            assertThat(activeDriver.getPenaltyPoints()).isEqualTo(5);

            verify(pointHistoryRepository).save(any(DriverPointHistoryEntity.class));
            verify(suspensionRepository, never()).save(any());
        }

        @Test
        void triggersSuspensionWhenThresholdReached() {
            activeDriver.setPenaltyPoints(9);
            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));
            when(suspensionRepository.save(any(LicenseSuspensionEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UUID violationId = UUID.randomUUID();
            DriverService.PenaltyPointsResult result =
                    driverService.applyPenaltyPoints(driverId, 3, "RED_LIGHT", violationId);

            assertThat(result.pointsAfter()).isEqualTo(12);
            assertThat(result.suspensionCreated()).isTrue();
            assertThat(result.suspension()).isNotNull();
            assertThat(result.suspension().getViolationId()).isEqualTo(violationId);
            assertThat(activeDriver.isSuspended()).isTrue();

            verify(suspensionRepository).save(any(LicenseSuspensionEntity.class));
        }

        @Test
        void doesNotDoubleSuspendWhenAlreadySuspendedAndOverThreshold() {
            suspendedDriver.setPenaltyPoints(15);
            when(driverRepository.findById(suspendedDriver.getId())).thenReturn(Optional.of(suspendedDriver));

            DriverService.PenaltyPointsResult result =
                    driverService.applyPenaltyPoints(suspendedDriver.getId(), 2, "SPEEDING", UUID.randomUUID());

            assertThat(result.suspensionCreated()).isFalse();
            verify(suspensionRepository, never()).save(any());
        }
    }

    // ───────────────────────────── removePenaltyPoints ─────────────────────────────

    @Nested
    class RemovePenaltyPointsTests {

        @Test
        void removesPointsNormally() {
            activeDriver.setPenaltyPoints(8);
            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));

            driverService.removePenaltyPoints(driverId, 3, "APPEAL_APPROVED APP-1");

            assertThat(activeDriver.getPenaltyPoints()).isEqualTo(5);
            verify(pointHistoryRepository).save(any(DriverPointHistoryEntity.class));
        }

        @Test
        void neverGoesBelowZero() {
            activeDriver.setPenaltyPoints(2);
            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));

            driverService.removePenaltyPoints(driverId, 10, "APPEAL_APPROVED APP-2");

            assertThat(activeDriver.getPenaltyPoints()).isEqualTo(0);
        }
    }

    // ───────────────────────────── suspend ─────────────────────────────

    @Nested
    class SuspendTests {

        @Test
        void throwsWhenAlreadySuspended() {
            SuspendDriverRequest req = new SuspendDriverRequest();
            req.setReason("Court order");

            when(driverRepository.findById(suspendedDriver.getId())).thenReturn(Optional.of(suspendedDriver));

            assertThatThrownBy(() -> driverService.suspend(suspendedDriver.getId(), req, null))
                    .isInstanceOf(DriverAlreadySuspendedException.class);

            verify(suspensionRepository, never()).save(any());
        }

        @Test
        void throwsWhenEndDateInPast() {
            SuspendDriverRequest req = new SuspendDriverRequest();
            req.setReason("Manual suspension");
            req.setEndDate(LocalDate.now().minusDays(1));

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));

            assertThatThrownBy(() -> driverService.suspend(driverId, req, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("future");
        }

        @Test
        void suspendsSuccessfullyAndResolvesOfficerFromPrincipal() {
            SuspendDriverRequest req = new SuspendDriverRequest();
            req.setReason("Court order");
            req.setEndDate(LocalDate.now().plusDays(90));
            UUID violationId = UUID.randomUUID();
            req.setViolationId(violationId);

            UserPrincipal principal = mock(UserPrincipal.class);
            UUID officerId = UUID.randomUUID();
            UserEntity officer = new UserEntity();
            officer.setId(officerId);

            when(principal.getId()).thenReturn(officerId);
            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));
            when(userRepository.findById(officerId)).thenReturn(Optional.of(officer));
            when(suspensionRepository.save(any(LicenseSuspensionEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            driverService.suspend(driverId, req, principal);

            assertThat(activeDriver.isSuspended()).isTrue();
            assertThat(activeDriver.getSuspendedUntil()).isEqualTo(req.getEndDate());

            verify(suspensionMapper).toDto(argThat(s ->
                    s.getSuspendedBy() == officer
                            && s.getViolationId().equals(violationId)
                            && s.isActive()));
        }

        @Test
        void suspendsSuccessfullyWithNullPrincipal() {
            SuspendDriverRequest req = new SuspendDriverRequest();
            req.setReason("System flagged");
            req.setEndDate(LocalDate.now().plusDays(30));

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));
            when(suspensionRepository.save(any(LicenseSuspensionEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            driverService.suspend(driverId, req, null);

            assertThat(activeDriver.isSuspended()).isTrue();
            verify(userRepository, never()).findById(any());
            verify(suspensionMapper).toDto(argThat(s -> s.getSuspendedBy() == null));
        }
    }

    // ───────────────────────────── liftSuspension ─────────────────────────────

    @Nested
    class LiftSuspensionTests {

        @Test
        void throwsWhenNotSuspended() {
            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));

            assertThatThrownBy(() -> driverService.liftSuspension(driverId))
                    .isInstanceOf(DriverNotSuspendedException.class);

            verify(suspensionRepository, never()).liftAllForDriver(any(), any());
        }

        @Test
        void liftsSuspensionSuccessfully() {
            UUID id = suspendedDriver.getId();
            when(driverRepository.findById(id)).thenReturn(Optional.of(suspendedDriver));

            driverService.liftSuspension(id);

            assertThat(suspendedDriver.isSuspended()).isFalse();
            assertThat(suspendedDriver.getSuspendedUntil()).isNull();
            verify(driverRepository).save(suspendedDriver);
            verify(suspensionRepository).liftAllForDriver(eq(id), any(LocalDate.class));
        }
    }

    // ───────────────────────────── linkUserAccount ─────────────────────────────

    @Nested
    class LinkUserAccountTests {

        @Test
        void throwsWhenDriverAlreadyHasLinkedUser() {
            UserEntity existingUser = new UserEntity();
            existingUser.setId(UUID.randomUUID());
            activeDriver.setUser(existingUser);

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));

            assertThatThrownBy(() -> driverService.linkUserAccount(driverId, UUID.randomUUID()))
                    .isInstanceOf(UserAlreadyLinkedToDriverException.class);
        }

        @Test
        void throwsWhenUserAlreadyLinkedToAnotherDriver() {
            UUID userId = UUID.randomUUID();

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));
            when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(suspendedDriver));

            assertThatThrownBy(() -> driverService.linkUserAccount(driverId, userId))
                    .isInstanceOf(DriverAlreadyLinkedException.class);

            verify(userRepository, never()).findById(any());
        }

        @Test
        void throwsWhenUserNotFound() {
            UUID userId = UUID.randomUUID();

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));
            when(driverRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> driverService.linkUserAccount(driverId, userId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void linksUserAccountSuccessfully() {
            UUID userId = UUID.randomUUID();
            UserEntity user = new UserEntity();
            user.setId(userId);

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));
            when(driverRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            driverService.linkUserAccount(driverId, userId);

            assertThat(activeDriver.getUser()).isEqualTo(user);
            verify(driverRepository).save(activeDriver);
        }
    }

    // ───────────────────────────── getDriverIdForUser ─────────────────────────────

    @Nested
    class GetDriverIdForUserTests {

        @Test
        void returnsDriverIdWhenLinked() {
            UUID userId = UUID.randomUUID();
            when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(activeDriver));

            UUID result = driverService.getDriverIdForUser(userId);

            assertThat(result).isEqualTo(driverId);
        }

        @Test
        void throwsWhenNoDriverLinkedToUser() {
            UUID userId = UUID.randomUUID();
            when(driverRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> driverService.getDriverIdForUser(userId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ───────────────────────────── liftExpiredSuspensions ─────────────────────────────
    // NOTE: this iterates suspensionRepository.findExpiredActiveSuspensions() and, for
    // each row, flips both the driver and the suspension. Assumes suspension.getDriver()
    // returns the owning DriverEntity (as wired in LicenseSuspensionEntity).

    @Nested
    class LiftExpiredSuspensionsTests {

        @Test
        void liftsAllExpiredSuspensionsAndTheirDrivers() {
            LicenseSuspensionEntity expired = new LicenseSuspensionEntity();
            expired.setDriver(suspendedDriver);
            expired.setActive(true);

            when(suspensionRepository.findExpiredActiveSuspensions(any(LocalDate.class)))
                    .thenReturn(List.of(expired));

            driverService.liftExpiredSuspensions();

            assertThat(suspendedDriver.isSuspended()).isFalse();
            assertThat(suspendedDriver.getSuspendedUntil()).isNull();
            assertThat(expired.isActive()).isFalse();
            assertThat(expired.getLiftedAt()).isEqualTo(LocalDate.now());

            verify(driverRepository).save(suspendedDriver);
            verify(suspensionRepository).save(expired);
        }

        @Test
        void doesNothingWhenNoExpiredSuspensions() {
            when(suspensionRepository.findExpiredActiveSuspensions(any(LocalDate.class)))
                    .thenReturn(List.of());

            driverService.liftExpiredSuspensions();

            verify(driverRepository, never()).save(any());
            verify(suspensionRepository, never()).save(any());
        }
    }

    // ───────────────────────────── resetAllPenaltyPoints ─────────────────────────────
    // NOTE: the in-memory loop filters findAll() for points > 0, zeroes + saves each,
    // writes a history row, then falls back to a bulk SQL update for anything missed.

    @Nested
    class ResetAllPenaltyPointsTests {

        @Test
        void resetsOnlyDriversWithNonZeroPoints() {
            DriverEntity zeroPointsDriver = new DriverEntity();
            zeroPointsDriver.setId(UUID.randomUUID());
            zeroPointsDriver.setPenaltyPoints(0);

            activeDriver.setPenaltyPoints(7);

            when(driverRepository.findAll()).thenReturn(List.of(activeDriver, zeroPointsDriver));

            driverService.resetAllPenaltyPoints();

            assertThat(activeDriver.getPenaltyPoints()).isEqualTo(0);
            verify(driverRepository).save(activeDriver);
            verify(driverRepository, never()).save(zeroPointsDriver);
            verify(pointHistoryRepository).save(any(DriverPointHistoryEntity.class));
            verify(driverRepository).resetAllPenaltyPoints();
        }
    }

    // ───────────────────────────── findByLicenseNumber ─────────────────────────────

    @Nested
    class FindByLicenseNumberTests {

        @Test
        void returnsDriverWhenFound() {
            when(driverRepository.findByLicenseNumber("LIC-001")).thenReturn(Optional.of(activeDriver));

            DriverEntity result = driverService.findByLicenseNumber("LIC-001");

            assertThat(result).isEqualTo(activeDriver);
        }

        @Test
        void throwsWhenNotFound() {
            when(driverRepository.findByLicenseNumber("MISSING")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> driverService.findByLicenseNumber("MISSING"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("MISSING");
        }
    }
}