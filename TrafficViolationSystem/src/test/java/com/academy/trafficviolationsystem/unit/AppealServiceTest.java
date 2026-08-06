package com.academy.trafficviolationsystem.unit;

import com.academy.trafficviolationsystem.appeal.*;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.appeal.*;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverRepository;
import com.academy.trafficviolationsystem.driver.DriverService;
import com.academy.trafficviolationsystem.fine.FineEntity;
import com.academy.trafficviolationsystem.fine.FineRepository;
import com.academy.trafficviolationsystem.fine.FineService;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.user.UserRepository;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import com.academy.trafficviolationsystem.violation.ViolationRepository;
import com.academy.trafficviolationsystem.violation.ViolationService;
import com.academy.trafficviolationsystem.violation.ViolationStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppealService.
 *
 * Target the logic AppealService actually owns: beforeInsert / afterInsert /
 * beforeUpdate hooks, the review workflow (startReview/approve/reject/withdraw),
 * additionalFilter, and the read helpers. insert()/update() themselves are
 * default methods on BaseCRUDService and are exercised indirectly through the
 * hooks, not re-tested directly.
 *
 * All collaborators are mocked — no Spring context, no database.
 *
 * NOTE: relies on findEntityById(), inherited from BaseCRUDService, assumed
 * here to resolve via appealRepository.findById(id) — same assumption made
 * in UserServiceTest for changePassword(). Share BaseCRUDService.java if it
 * works differently and these will need adjusting.
 *
 * NOTE: beforeInsert()/beforeUpdate() read the current principal from
 * SecurityContextHolder directly (a static call), so those tests populate a
 * real SecurityContext via setAuthenticatedPrincipal() and clear it in
 * tearDown(). startReview/approve/reject/withdraw take the principal as a
 * method parameter instead and don't need this.
 */
@ExtendWith(MockitoExtension.class)
class AppealServiceTest {

    @Mock private AppealRepository appealRepository;
    @Mock private ViolationRepository violationRepository;
    @Mock private ViolationService violationService;
    @Mock private FineRepository fineRepository;
    @Mock private FineService fineService;
    @Mock private DriverRepository driverRepository;
    @Mock private DriverService driverService;
    @Mock private UserRepository userRepository;
    @Mock private AppealMapper appealMapper;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private AppealService appealService;

    private UUID violationId;
    private UUID driverId;
    private UUID citizenUserId;
    private UUID officerUserId;

    private ViolationEntity violation;
    private DriverEntity driver;
    private UserPrincipal citizenPrincipal;
    private UserPrincipal officerPrincipal;
    private AppealCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        violationId = UUID.randomUUID();
        driverId = UUID.randomUUID();
        citizenUserId = UUID.randomUUID();
        officerUserId = UUID.randomUUID();

        violation = mock(ViolationEntity.class);
        driver = mock(DriverEntity.class);
        citizenPrincipal = mock(UserPrincipal.class);
        officerPrincipal = mock(UserPrincipal.class);

        // Happy-path defaults — individual tests override with a fresh
        // when(...) call whenever they need a different value.
        lenient().when(violation.getId()).thenReturn(violationId);
        lenient().when(violation.getDriver()).thenReturn(driver);
        lenient().when(violation.getStatus()).thenReturn(ViolationStatus.CONFIRMED);
        lenient().when(violation.getOccurredAt()).thenReturn(LocalDateTime.now().minusDays(5));
        lenient().when(violation.getReferenceNumber()).thenReturn("V-2025-000123");

        lenient().when(driver.getId()).thenReturn(driverId);

        lenient().when(citizenPrincipal.isCitizen()).thenReturn(true);
        lenient().when(citizenPrincipal.getId()).thenReturn(citizenUserId);

        lenient().when(officerPrincipal.isCitizen()).thenReturn(false);
        lenient().when(officerPrincipal.getId()).thenReturn(officerUserId);

        validRequest = new AppealCreateRequest();
        validRequest.setViolationId(violationId);
        validRequest.setReason("The sign was obscured by a tree branch at the time of the alleged violation.");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedPrincipal(UserPrincipal principal) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    // ───────────────────────────── beforeInsert ─────────────────────────────

    @Nested
    class BeforeInsertTests {

        @Test
        void throwsWhenViolationNotFound() {
            //setAuthenticatedPrincipal(officerPrincipal);
            when(violationRepository.findById(violationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appealService.beforeInsert(validRequest, new ViolationAppealEntity()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsWhenCitizenDoesNotOwnDriver() {
            setAuthenticatedPrincipal(citizenPrincipal);
            when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));
            when(driverRepository.findByUserId(citizenUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appealService.beforeInsert(validRequest, new ViolationAppealEntity()))
                    .isInstanceOf(AppealAccessDeniedException.class);
        }

        @Test
        void allowsCitizenFilingForOwnViolation() {
            setAuthenticatedPrincipal(citizenPrincipal);
            when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));
            when(driverRepository.findByUserId(citizenUserId)).thenReturn(Optional.of(driver));
            when(appealRepository.countByYear(any(), any())).thenReturn(0L);

            ViolationAppealEntity entity = new ViolationAppealEntity();
            appealService.beforeInsert(validRequest, entity);

            assertThat(entity.getDriver()).isEqualTo(driver);
            assertThat(entity.getStatus()).isEqualTo(AppealStatus.SUBMITTED);
        }

        @Test
        void throwsWhenViolationNotAppealable() {
            setAuthenticatedPrincipal(officerPrincipal);
            when(violation.getStatus()).thenReturn(ViolationStatus.DISMISSED);
            when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));

            assertThatThrownBy(() -> appealService.beforeInsert(validRequest, new ViolationAppealEntity()))
                    .isInstanceOf(ViolationNotAppealableException.class);
        }

        @Test
        void throwsWhenNoDriverAssigned() {
            // Officer principal — verifyCitizenOwnsDriver is a no-op for
            // non-citizens, so a null driver here won't NPE ahead of the
            // explicit null check.
            setAuthenticatedPrincipal(officerPrincipal);
            when(violation.getDriver()).thenReturn(null);
            when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));

            assertThatThrownBy(() -> appealService.beforeInsert(validRequest, new ViolationAppealEntity()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("no driver is assigned");
        }

        @Test
        void throwsWhenActiveAppealAlreadyExists() {
            setAuthenticatedPrincipal(officerPrincipal);
            when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));

            ViolationAppealEntity existing = new ViolationAppealEntity();
            existing.setAppealNumber("APP-2025-000001");
            when(appealRepository.findActiveByViolationId(violationId)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> appealService.beforeInsert(validRequest, new ViolationAppealEntity()))
                    .isInstanceOf(AppealAlreadyExistsException.class);
        }

        @Test
        void throwsWhenAppealWindowClosed() {
            setAuthenticatedPrincipal(officerPrincipal);
            when(violation.getOccurredAt()).thenReturn(LocalDateTime.now().minusDays(40));
            when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));

            assertThatThrownBy(() -> appealService.beforeInsert(validRequest, new ViolationAppealEntity()))
                    .isInstanceOf(AppealWindowClosedException.class);
        }

        @Test
        void setsRelationshipsAndMetadataForValidRequest() {
            setAuthenticatedPrincipal(officerPrincipal);
            when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));
            when(fineRepository.findByViolationId(violationId)).thenReturn(Optional.empty());
            when(appealRepository.countByYear(any(), any())).thenReturn(4L);

            ViolationAppealEntity entity = new ViolationAppealEntity();
            appealService.beforeInsert(validRequest, entity);

            assertThat(entity.getViolation()).isEqualTo(violation);
            assertThat(entity.getDriver()).isEqualTo(driver);
            assertThat(entity.getStatus()).isEqualTo(AppealStatus.SUBMITTED);
            assertThat(entity.getSubmittedAt()).isNotNull();
            assertThat(entity.getAppealNumber()).matches("APP-\\d{4}-000005");
            assertThat(entity.getFineId()).isNull();
        }

        @Test
        void linksFineIdWhenFineExistsForViolation() {
            setAuthenticatedPrincipal(officerPrincipal);
            when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));

            UUID fineId = UUID.randomUUID();
            FineEntity fine = mock(FineEntity.class);
            when(fine.getId()).thenReturn(fineId);
            when(fineRepository.findByViolationId(violationId)).thenReturn(Optional.of(fine));
            when(appealRepository.countByYear(any(), any())).thenReturn(0L);

            ViolationAppealEntity entity = new ViolationAppealEntity();
            appealService.beforeInsert(validRequest, entity);

            assertThat(entity.getFineId()).isEqualTo(fineId);
        }
    }

    // ───────────────────────────── afterInsert ─────────────────────────────

    @Nested
    class AfterInsertTests {

        @Test
        void marksViolationAndFineDisputedWhenFineExists() {
            ViolationAppealEntity entity = new ViolationAppealEntity();
            entity.setViolation(violation);
            UUID fineId = UUID.randomUUID();
            entity.setFineId(fineId);

            appealService.afterInsert(validRequest, entity);

            verify(violationService).markDisputed(violationId);
            verify(fineService).markDisputed(fineId);
        }

        @Test
        void marksOnlyViolationDisputedWhenNoFineLinked() {
            ViolationAppealEntity entity = new ViolationAppealEntity();
            entity.setViolation(violation);
            entity.setFineId(null);

            appealService.afterInsert(validRequest, entity);

            verify(violationService).markDisputed(violationId);
            verify(fineService, never()).markDisputed(any());
        }
    }

    // ───────────────────────────── beforeUpdate ─────────────────────────────

    @Nested
    class BeforeUpdateTests {

        @Test
        void allowsUpdateWhenSubmittedAndOwnerMatches() {
            setAuthenticatedPrincipal(officerPrincipal);
            ViolationAppealEntity entity = new ViolationAppealEntity();
            entity.setStatus(AppealStatus.SUBMITTED);
            entity.setDriver(driver);
            entity.setAppealNumber("APP-2025-000001");

            appealService.beforeUpdate(new AppealUpdateRequest(), entity); // should not throw
        }

        @Test
        void throwsWhenNoLongerEditable() {
            //setAuthenticatedPrincipal(officerPrincipal);
            ViolationAppealEntity entity = new ViolationAppealEntity();
            entity.setStatus(AppealStatus.UNDER_REVIEW);
            entity.setDriver(driver);
            entity.setAppealNumber("APP-2025-000001");

            assertThatThrownBy(() -> appealService.beforeUpdate(new AppealUpdateRequest(), entity))
                    .isInstanceOf(InvalidAppealStatusException.class)
                    .hasMessageContaining("no longer be edited");
        }

        @Test
        void throwsWhenCitizenDoesNotOwnDriver() {
            setAuthenticatedPrincipal(citizenPrincipal);
            when(driverRepository.findByUserId(citizenUserId)).thenReturn(Optional.empty());

            ViolationAppealEntity entity = new ViolationAppealEntity();
            entity.setStatus(AppealStatus.SUBMITTED);
            entity.setDriver(driver);
            entity.setAppealNumber("APP-2025-000001");

            assertThatThrownBy(() -> appealService.beforeUpdate(new AppealUpdateRequest(), entity))
                    .isInstanceOf(AppealAccessDeniedException.class);
        }
    }

    // ───────────────────────────── additionalFilter ─────────────────────────────
    // Pure logic tests against the mocked Criteria API — no DB, no Spring context.

    @Nested
    class AdditionalFilterTests {

        @Test
        void returnsEmptyListWhenNoFiltersProvided() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<ViolationAppealEntity> root = mock(Root.class);
            AppealSearchObject searchObj = new AppealSearchObject();

            List<Predicate> predicates = appealService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void addsStatusFilterWhenPresent() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<ViolationAppealEntity> root = mock(Root.class);
            Path<Object> statusPath = mock(Path.class);
            Predicate statusPredicate = mock(Predicate.class);

            when(root.<Object>get("status")).thenReturn(statusPath);
            when(cb.equal(statusPath, AppealStatus.SUBMITTED)).thenReturn(statusPredicate);

            AppealSearchObject searchObj = new AppealSearchObject();
            searchObj.setStatus(AppealStatus.SUBMITTED);

            List<Predicate> predicates = appealService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactly(statusPredicate);
        }

        @Test
        @SuppressWarnings("unchecked")
        void addsDriverFilterUsingNestedPath() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<ViolationAppealEntity> root = mock(Root.class);
            Path<Object> driverPath = mock(Path.class);
            Path<Object> driverIdPath = mock(Path.class);
            Predicate driverPredicate = mock(Predicate.class);

            when(root.<Object>get("driver")).thenReturn(driverPath);
            when(driverPath.get("id")).thenReturn(driverIdPath);
            UUID filterDriverId = UUID.randomUUID();
            when(cb.equal(driverIdPath, filterDriverId)).thenReturn(driverPredicate);

            AppealSearchObject searchObj = new AppealSearchObject();
            searchObj.setDriverId(filterDriverId);

            List<Predicate> predicates = appealService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactly(driverPredicate);
        }

        @Test
        @SuppressWarnings("unchecked")
        void addsViolationFilterUsingNestedPath() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<ViolationAppealEntity> root = mock(Root.class);
            Path<Object> violationPath = mock(Path.class);
            Path<Object> violationIdPath = mock(Path.class);
            Predicate violationPredicate = mock(Predicate.class);

            when(root.<Object>get("violation")).thenReturn(violationPath);
            when(violationPath.get("id")).thenReturn(violationIdPath);
            UUID filterViolationId = UUID.randomUUID();
            when(cb.equal(violationIdPath, filterViolationId)).thenReturn(violationPredicate);

            AppealSearchObject searchObj = new AppealSearchObject();
            searchObj.setViolationId(filterViolationId);

            List<Predicate> predicates = appealService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactly(violationPredicate);
        }

        @Test
        @SuppressWarnings("unchecked")
        void addsReviewedByFilterUsingNestedPath() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<ViolationAppealEntity> root = mock(Root.class);
            Path<Object> reviewedByPath = mock(Path.class);
            Path<Object> reviewedByIdPath = mock(Path.class);
            Predicate reviewedByPredicate = mock(Predicate.class);

            when(root.<Object>get("reviewedBy")).thenReturn(reviewedByPath);
            when(reviewedByPath.get("id")).thenReturn(reviewedByIdPath);
            UUID filterReviewerId = UUID.randomUUID();
            when(cb.equal(reviewedByIdPath, filterReviewerId)).thenReturn(reviewedByPredicate);

            AppealSearchObject searchObj = new AppealSearchObject();
            searchObj.setReviewedById(filterReviewerId);

            List<Predicate> predicates = appealService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactly(reviewedByPredicate);
        }

        @Test
        @SuppressWarnings("unchecked")
        void computesInclusiveDateRangeBoundaries() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<ViolationAppealEntity> root = mock(Root.class);

            // 1. Strongly type the Path mock to LocalDateTime instead of Object
            Path<LocalDateTime> submittedAtPath = mock(Path.class);

            // 2. Strongly type the root.get call to match the expected LocalDateTime return type
            when(root.<LocalDateTime>get("submittedAt")).thenReturn(submittedAtPath);

            LocalDate from = LocalDate.of(2025, 3, 1);
            LocalDate to = LocalDate.of(2025, 3, 31);

            AppealSearchObject searchObj = new AppealSearchObject();
            searchObj.setFromDate(from);
            searchObj.setToDate(to);

            appealService.additionalFilter(cb, searchObj, root);

            // fromDate is inclusive at start-of-day; toDate is inclusive
            // through end-of-day, implemented as < (toDate + 1 day).
            verify(cb).greaterThanOrEqualTo(submittedAtPath, from.atStartOfDay());
            verify(cb).lessThan(submittedAtPath, to.plusDays(1).atStartOfDay());
        }
    }

    // ───────────────────────────── startReview ─────────────────────────────

    @Nested
    class StartReviewTests {

        @Test
        void transitionsSubmittedToUnderReviewAndSetsReviewer() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setStatus(AppealStatus.SUBMITTED);
            appeal.setAppealNumber("APP-2025-000001");

            UUID appealId = UUID.randomUUID();
            when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));

            UserEntity reviewer = mock(UserEntity.class);
            when(userRepository.findById(officerUserId)).thenReturn(Optional.of(reviewer));
            when(appealMapper.toDto(appeal)).thenReturn(new AppealDto());

            appealService.startReview(appealId, officerPrincipal);

            assertThat(appeal.getStatus()).isEqualTo(AppealStatus.UNDER_REVIEW);
            assertThat(appeal.getReviewedBy()).isEqualTo(reviewer);
            verify(appealRepository).save(appeal);
        }

        @Test
        void throwsWhenAlreadyUnderReview() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setStatus(AppealStatus.UNDER_REVIEW);
            appeal.setAppealNumber("APP-2025-000001");

            UUID appealId = UUID.randomUUID();
            when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));

            assertThatThrownBy(() -> appealService.startReview(appealId, officerPrincipal))
                    .isInstanceOf(InvalidAppealStatusException.class)
                    .hasMessageContaining("already UNDER_REVIEW");

            verify(appealRepository, never()).save(any());
        }
    }

    // ───────────────────────────── approve ─────────────────────────────

    @Nested
    class ApproveTests {

        private ReviewAppealRequest request;

        @BeforeEach
        void initRequest() {
            request = new ReviewAppealRequest();
            request.setReviewNotes("Sign was confirmed obscured by a tree branch on-site.");
        }

        @Test
        void approvesFromSubmittedCancelsFineAndDismissesViolation() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setStatus(AppealStatus.SUBMITTED);
            appeal.setAppealNumber("APP-2025-000001");
            appeal.setViolation(violation);
            UUID fineId = UUID.randomUUID();
            appeal.setFineId(fineId);

            UUID appealId = UUID.randomUUID();
            when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));

            UserEntity reviewer = mock(UserEntity.class);
            when(userRepository.findById(officerUserId)).thenReturn(Optional.of(reviewer));
            when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));
            when(appealMapper.toDto(appeal)).thenReturn(new AppealDto());

            appealService.approve(appealId, request, officerPrincipal);

            assertThat(appeal.getStatus()).isEqualTo(AppealStatus.APPROVED);
            assertThat(appeal.getReviewedBy()).isEqualTo(reviewer);
            assertThat(appeal.getReviewedAt()).isNotNull();
            assertThat(appeal.getReviewNotes()).isEqualTo(request.getReviewNotes());

            verify(fineService).cancel(eq(fineId), contains(appeal.getAppealNumber()), eq(officerPrincipal));
            verify(violation).setStatus(ViolationStatus.DISMISSED);
            verify(violation).setReviewedBy(reviewer);
            verify(violationRepository).save(violation);
        }

        @Test
        void skipsFineCancelWhenNoFineLinked() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setStatus(AppealStatus.SUBMITTED);
            appeal.setAppealNumber("APP-2025-000002");
            appeal.setViolation(violation);
            appeal.setFineId(null);

            UUID appealId = UUID.randomUUID();
            when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));

            UserEntity reviewer = mock(UserEntity.class);
            when(userRepository.findById(officerUserId)).thenReturn(Optional.of(reviewer));
            when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));
            when(appealMapper.toDto(appeal)).thenReturn(new AppealDto());

            appealService.approve(appealId, request, officerPrincipal);

            verify(fineService, never()).cancel(any(), any(), any());
        }

        @Test
        void throwsWhenAppealAlreadyDecided() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setStatus(AppealStatus.REJECTED);
            appeal.setAppealNumber("APP-2025-000003");

            UUID appealId = UUID.randomUUID();
            when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));

            assertThatThrownBy(() -> appealService.approve(appealId, request, officerPrincipal))
                    .isInstanceOf(InvalidAppealStatusException.class)
                    .hasMessageContaining("already been rejected");

            verify(appealRepository, never()).save(any());
            verify(fineService, never()).cancel(any(), any(), any());
        }
    }

    // ───────────────────────────── reject ─────────────────────────────

    @Nested
    class RejectTests {

        private ReviewAppealRequest request;

        @BeforeEach
        void initRequest() {
            request = new ReviewAppealRequest();
            request.setReviewNotes("Evidence submitted did not support the driver's claim.");
        }

        @Test
        void rejectsFromUnderReviewReinstatesFineAndViolation() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setStatus(AppealStatus.UNDER_REVIEW);
            appeal.setAppealNumber("APP-2025-000010");
            appeal.setViolation(violation);
            UUID fineId = UUID.randomUUID();
            appeal.setFineId(fineId);

            UUID appealId = UUID.randomUUID();
            when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));

            UserEntity reviewer = mock(UserEntity.class);
            when(userRepository.findById(officerUserId)).thenReturn(Optional.of(reviewer));
            when(appealMapper.toDto(appeal)).thenReturn(new AppealDto());

            appealService.reject(appealId, request, officerPrincipal);

            assertThat(appeal.getStatus()).isEqualTo(AppealStatus.REJECTED);
            assertThat(appeal.getReviewNotes()).isEqualTo(request.getReviewNotes());
            verify(fineService).reinstateAfterAppealRejection(fineId);
            verify(violationService).reinstateAfterAppealRejection(violationId);
        }

        @Test
        void skipsFineReinstateWhenNoFineLinked() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setStatus(AppealStatus.SUBMITTED);
            appeal.setAppealNumber("APP-2025-000011");
            appeal.setViolation(violation);
            appeal.setFineId(null);

            UUID appealId = UUID.randomUUID();
            when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));

            UserEntity reviewer = mock(UserEntity.class);
            when(userRepository.findById(officerUserId)).thenReturn(Optional.of(reviewer));
            when(appealMapper.toDto(appeal)).thenReturn(new AppealDto());

            appealService.reject(appealId, request, officerPrincipal);

            verify(fineService, never()).reinstateAfterAppealRejection(any());
            verify(violationService).reinstateAfterAppealRejection(violationId);
        }

        @Test
        void throwsWhenAlreadyApproved() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setStatus(AppealStatus.APPROVED);
            appeal.setAppealNumber("APP-2025-000012");

            UUID appealId = UUID.randomUUID();
            when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));

            assertThatThrownBy(() -> appealService.reject(appealId, request, officerPrincipal))
                    .isInstanceOf(InvalidAppealStatusException.class)
                    .hasMessageContaining("already been approved");

            verify(appealRepository, never()).save(any());
        }
    }

    // ───────────────────────────── withdraw ─────────────────────────────

    @Nested
    class WithdrawTests {

        @Test
        void withdrawsFromSubmittedReinstatesFineAndViolation() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setStatus(AppealStatus.SUBMITTED);
            appeal.setAppealNumber("APP-2025-000020");
            appeal.setDriver(driver);
            appeal.setViolation(violation);
            UUID fineId = UUID.randomUUID();
            appeal.setFineId(fineId);

            UUID appealId = UUID.randomUUID();
            when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));
            when(driverRepository.findByUserId(citizenUserId)).thenReturn(Optional.of(driver));
            when(appealMapper.toDto(appeal)).thenReturn(new AppealDto());

            appealService.withdraw(appealId, citizenPrincipal);

            assertThat(appeal.getStatus()).isEqualTo(AppealStatus.WITHDRAWN);
            assertThat(appeal.getReviewNotes()).isEqualTo("Withdrawn by driver");
            assertThat(appeal.getReviewedAt()).isNotNull();
            verify(fineService).reinstateAfterAppealRejection(fineId);
            verify(violationService).reinstateAfterAppealRejection(violationId);
        }

        @Test
        void throwsWhenCitizenWithdrawingSomeoneElsesAppeal() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setStatus(AppealStatus.SUBMITTED);
            appeal.setAppealNumber("APP-2025-000021");
            appeal.setDriver(driver);

            UUID appealId = UUID.randomUUID();
            when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));
            when(driverRepository.findByUserId(citizenUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appealService.withdraw(appealId, citizenPrincipal))
                    .isInstanceOf(AppealAccessDeniedException.class);

            verify(appealRepository, never()).save(any());
        }

        @Test
        void throwsWhenNotInSubmittedStatus() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setStatus(AppealStatus.UNDER_REVIEW);
            appeal.setAppealNumber("APP-2025-000022");
            appeal.setDriver(driver);

            UUID appealId = UUID.randomUUID();
            when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));

            // Officer principal — isCitizen() is false, so the ownership
            // check is skipped and only the state transition is exercised.
            assertThatThrownBy(() -> appealService.withdraw(appealId, officerPrincipal))
                    .isInstanceOf(InvalidAppealStatusException.class)
                    .hasMessageContaining("can only be withdrawn while in SUBMITTED status");
        }
    }

    // ───────────────────────────── toDtoWithDetails ─────────────────────────────

    @Nested
    class ToDtoWithDetailsTests {

        @Test
        void populatesViolationReferenceWhenViolationPresent() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            appeal.setViolation(violation);
            AppealDto dto = new AppealDto();
            when(appealMapper.toDto(appeal)).thenReturn(dto);

            AppealDto result = appealService.toDtoWithDetails(appeal);

            assertThat(result.getViolationReference()).isEqualTo(violation.getReferenceNumber());
        }

        @Test
        void leavesViolationReferenceNullWhenNoViolation() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            AppealDto dto = new AppealDto();
            when(appealMapper.toDto(appeal)).thenReturn(dto);

            AppealDto result = appealService.toDtoWithDetails(appeal);

            assertThat(result.getViolationReference()).isNull();
        }
    }

    // ───────────────────────────── read helpers ─────────────────────────────

    @Nested
    class GetForDriverTests {

        @Test
        void returnsDriverAppealsMappedToDto() {
            UUID targetDriverId = UUID.randomUUID();

            ViolationAppealEntity appeal1 = new ViolationAppealEntity();
            appeal1.setId(UUID.randomUUID()); // Give distinct identity

            ViolationAppealEntity appeal2 = new ViolationAppealEntity();
            appeal2.setId(UUID.randomUUID()); // Give distinct identity

            when(appealRepository.findByDriverIdOrderBySubmittedAtDesc(targetDriverId))
                    .thenReturn(List.of(appeal1, appeal2));
            when(appealMapper.toDto(appeal1)).thenReturn(new AppealDto());
            when(appealMapper.toDto(appeal2)).thenReturn(new AppealDto());

            List<AppealDto> result = appealService.getForDriver(targetDriverId);

            assertThat(result).hasSize(2);

            // Now Mockito can easily distinguish between appeal1 and appeal2
            verify(appealMapper).toDto(appeal1);
            verify(appealMapper).toDto(appeal2);
        }
    }

    @Nested
    class GetPendingReviewQueueTests {

        @Test
        void returnsSubmittedAppealsOldestFirst() {
            ViolationAppealEntity appeal = new ViolationAppealEntity();
            when(appealRepository.findByStatusOrderBySubmittedAtAsc(AppealStatus.SUBMITTED))
                    .thenReturn(List.of(appeal));
            when(appealMapper.toDto(appeal)).thenReturn(new AppealDto());

            List<AppealDto> result = appealService.getPendingReviewQueue();

            assertThat(result).hasSize(1);
            verify(appealRepository).findByStatusOrderBySubmittedAtAsc(AppealStatus.SUBMITTED);
        }
    }
}