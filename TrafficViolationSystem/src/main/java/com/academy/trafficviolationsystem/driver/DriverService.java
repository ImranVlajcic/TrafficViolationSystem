package com.academy.trafficviolationsystem.driver;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.driver.DriverAlreadyLinkedException;
import com.academy.trafficviolationsystem.core.exceptions.driver.DriverAlreadySuspendedException;
import com.academy.trafficviolationsystem.core.exceptions.driver.DriverNotSuspendedException;
import com.academy.trafficviolationsystem.core.exceptions.driver.UserAlreadyLinkedToDriverException;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import com.academy.trafficviolationsystem.core.services.BaseCRUDService;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for driver management.
 *
 * Implements BaseCRUDService — insert(), update(), search(), findById()
 * are handled by the base. Domain-specific methods are defined below.
 *
 * Key operations beyond CRUD:
 *   applyPenaltyPoints  — called by FineService after a fine is issued
 *   removePenaltyPoints — called when an appeal is approved
 *   suspend             — manual suspension by an officer
 *   liftSuspension      — manual or job-triggered suspension lift
 *   linkUserAccount     — connects a citizen UserEntity to a DriverEntity
 *   resetAllPoints      — called annually by LicensePointResetJob
 */
@Service
@Transactional
public class DriverService implements BaseCRUDService<
        DriverEntity, DriverDto, DriverSearchObject, DriverCreateRequest, DriverUpdateRequest, UUID> {

    // Threshold from SystemConfig — hardcoded here as a constant for now;
    // replace with systemConfigService.getInt("SUSPENSION_THRESHOLD_POINTS") later.
    private static final int SUSPENSION_THRESHOLD  = 12;
    private static final int SUSPENSION_DAYS       = 90;

    private final DriverRepository              driverRepository;
    private final LicenseSuspensionRepository   suspensionRepository;
    private final DriverPointHistoryRepository  pointHistoryRepository;
    private final UserRepository                userRepository;
    private final DriverMapper                  driverMapper;
    private final LicenseSuspensionMapper       suspensionMapper;
    private final DriverPointHistoryMapper      pointHistoryMapper;
    private final EntityManager                 entityManager;

    public DriverService(DriverRepository driverRepository,
                         LicenseSuspensionRepository suspensionRepository,
                         DriverPointHistoryRepository pointHistoryRepository,
                         UserRepository userRepository,
                         DriverMapper driverMapper,
                         LicenseSuspensionMapper suspensionMapper,
                         DriverPointHistoryMapper pointHistoryMapper,
                         EntityManager entityManager) {
        this.driverRepository      = driverRepository;
        this.suspensionRepository  = suspensionRepository;
        this.pointHistoryRepository= pointHistoryRepository;
        this.userRepository        = userRepository;
        this.driverMapper          = driverMapper;
        this.suspensionMapper      = suspensionMapper;
        this.pointHistoryMapper    = pointHistoryMapper;
        this.entityManager         = entityManager;
    }

    // ── BaseCRUDService wiring ────────────────────────────────────────────

    @Override public DriverRepository    getRepository()    { return driverRepository; }
    @Override public EntityManager       getEntityManager() { return entityManager;    }
    @Override public DriverMapper        getMapper()        { return driverMapper;     }
    @Override public Class<DriverEntity> getEntityClass()   { return DriverEntity.class; }

    // ── lifecycle hooks ───────────────────────────────────────────────────

    @Override
    @AuditAction(value = "REGISTER_DRIVER", entityClass = DriverEntity.class)
    public DriverDto insert(DriverCreateRequest request) {
        return BaseCRUDService.super.insert(request);
    }

    @Override
    @AuditAction(value = "UPDATE_DRIVER", entityClass = DriverEntity.class)
    public DriverDto update(UUID id, DriverUpdateRequest request) {
        return BaseCRUDService.super.update(id, request);
    }

    @Override
    public void beforeInsert(DriverCreateRequest request, DriverEntity entity) {
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new BadRequestException(
                "License number '" + request.getLicenseNumber() + "' is already registered");
        }
        if (driverRepository.existsByNationalId(request.getNationalId())) {
            throw new BadRequestException(
                "National ID '" + request.getNationalId() + "' is already registered");
        }
        if (request.getLicenseExpiresAt().isBefore(request.getLicenseIssuedAt())) {
            throw new BadRequestException("License expiry date must be after the issued date");
        }
    }

    @Override
    public void beforeUpdate(DriverUpdateRequest request, DriverEntity entity) {
        if (request.getLicenseIssuedAt() != null && request.getLicenseExpiresAt() != null) {
            if (request.getLicenseExpiresAt().isBefore(request.getLicenseIssuedAt())) {
                throw new BadRequestException("License expiry date must be after the issued date");
            }
        }
        // If only one date is being updated, validate against the persisted other date
        if (request.getLicenseExpiresAt() != null && request.getLicenseIssuedAt() == null) {
            if (request.getLicenseExpiresAt().isBefore(entity.getLicenseIssuedAt())) {
                throw new BadRequestException("License expiry date must be after the issued date");
            }
        }
    }

    // ── search filters ────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                            DriverSearchObject searchObj,
                                            Root<DriverEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getSearch() != null && !searchObj.getSearch().isBlank()) {
            String pattern = "%" + searchObj.getSearch().toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("firstName")),     pattern),
                cb.like(cb.lower(root.get("lastName")),      pattern),
                cb.like(cb.lower(root.get("licenseNumber")), pattern),
                cb.like(cb.lower(root.get("nationalId")),    pattern),
                cb.like(cb.lower(root.get("email")),         pattern)
            ));
        }

        if (searchObj.getIsSuspended() != null) {
            predicates.add(cb.equal(root.get("isSuspended"), searchObj.getIsSuspended()));
        }

        if (searchObj.getLicenseCategory() != null && !searchObj.getLicenseCategory().isBlank()) {
            predicates.add(cb.equal(
                cb.lower(root.get("licenseCategory")),
                searchObj.getLicenseCategory().toLowerCase()
            ));
        }

        if (searchObj.getLicenseExpired() != null) {
            if (Boolean.TRUE.equals(searchObj.getLicenseExpired())) {
                predicates.add(cb.lessThan(root.get("licenseExpiresAt"), LocalDate.now()));
            } else {
                predicates.add(cb.greaterThanOrEqualTo(root.get("licenseExpiresAt"), LocalDate.now()));
            }
        }

        return predicates;
    }

    // ── penalty point operations ──────────────────────────────────────────

    /** Result of applying penalty points — tells the caller whether the
     *  threshold was crossed and a suspension was created, so orchestration
     *  (e.g. sending a suspension notification) can happen without
     *  DriverService needing to know about NotificationService. */
    public record PenaltyPointsResult(int pointsBefore, int pointsAfter,
                                      boolean suspensionCreated,
                                      LicenseSuspensionEntity suspension) {}

    @Transactional
    @AuditAction(value = "APPLY_PENALTY_POINTS", entityClass = DriverEntity.class)
    public PenaltyPointsResult applyPenaltyPoints(UUID driverId, int points, String reason, UUID violationId) {
        DriverEntity driver = findEntityById(driverId);

        int before = driver.getPenaltyPoints();
        int after  = before + points;
        driver.setPenaltyPoints(after);
        driverRepository.save(driver);

        writePointHistory(driver, points, before, after, reason, violationId);

        LicenseSuspensionEntity suspension = null;
        if (after >= SUSPENSION_THRESHOLD && !driver.isSuspended()) {
            suspension = suspendInternal(driver, "Automatic suspension: penalty point threshold reached",
                    LocalDate.now().plusDays(SUSPENSION_DAYS), violationId, null);
        }

        return new PenaltyPointsResult(before, after, suspension != null, suspension);
    }

    /**
     * Removes penalty points from a driver — called when an appeal is approved.
     *
     * @param driverId  the driver
     * @param points    positive number of points to remove
     * @param reason    e.g. "APPEAL_APPROVED APP-2025-001"
     */
    @Transactional
    @AuditAction(value = "REMOVE_PENALTY_POINTS", entityClass = DriverEntity.class)
    public void removePenaltyPoints(UUID driverId, int points, String reason) {
        DriverEntity driver = findEntityById(driverId);

        int before = driver.getPenaltyPoints();
        int after  = Math.max(0, before - points); // never go below 0
        driver.setPenaltyPoints(after);
        driverRepository.save(driver);

        writePointHistory(driver, -points, before, after, reason, null);
    }

    // ── suspension operations ─────────────────────────────────────────────

    /**
     * Manually suspends a driver's license.
     * Called by DriverController POST /api/drivers/{id}/suspend.
     */
    @Transactional
    @AuditAction(value = "SUSPEND_DRIVER", entityClass = DriverEntity.class)
    public LicenseSuspensionDto suspend(UUID driverId, SuspendDriverRequest request,
                                        UserPrincipal principal) {
        DriverEntity driver = findEntityById(driverId);

        if (driver.isSuspended()) {
            throw new DriverAlreadySuspendedException(driverId);
        }
        if (request.getEndDate() != null && request.getEndDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Suspension end date must be in the future");
        }

        UserEntity officer = principal != null
                ? userRepository.findById(principal.getId()).orElse(null)
                : null;

        LicenseSuspensionEntity suspension = suspendInternal(
                driver, request.getReason(), request.getEndDate(),
                request.getViolationId(), officer);

        return suspensionMapper.toDto(suspension);
    }

    /**
     * Lifts a driver's current suspension.
     * Called by DriverController POST /api/drivers/{id}/lift-suspension
     * or by LicensePointResetJob.
     */
    @Transactional
    public DriverDto liftSuspension(UUID driverId) {
        DriverEntity driver = findEntityById(driverId);

        if (!driver.isSuspended()) {
            throw new DriverNotSuspendedException(driverId);
        }

        driver.setSuspended(false);
        driver.setSuspendedUntil(null);
        driverRepository.save(driver);

        suspensionRepository.liftAllForDriver(driverId, LocalDate.now());

        return driverMapper.toDto(driver);
    }

    // ── portal account link ───────────────────────────────────────────────

    /**
     * Links a citizen UserEntity to this driver record.
     * Called when a citizen registers on the portal and provides their license number.
     */
    @Transactional
    @AuditAction(value = "LINK_DRIVER_USER_ACCOUNT", entityClass = DriverEntity.class)
    public DriverDto linkUserAccount(UUID driverId, UUID userId) {
        DriverEntity driver = findEntityById(driverId);

        if (driver.getUser() != null) {
            throw new UserAlreadyLinkedToDriverException(userId);
        }

        // Check the user isn't already linked to a different driver
        if (driverRepository.findByUserId(userId).isPresent()) {
            throw new DriverAlreadyLinkedException(driverId);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));

        driver.setUser(user);
        driverRepository.save(driver);
        return driverMapper.toDto(driver);
    }

    // ── read helpers (used by other modules) ──────────────────────────────

    /**
     * Looks up a driver by their license plate number — used by ViolationService
     * and CameraEventProcessorService when identifying the owner of a detected vehicle.
     */
    @Transactional(readOnly = true)
    public DriverEntity findByLicenseNumber(String licenseNumber) {
        return driverRepository.findByLicenseNumber(licenseNumber)
                .orElseThrow(() -> new NotFoundException(
                    "No driver found with license number: " + licenseNumber));
    }

    /** Returns the suspension history for a driver — used by DriverController. */
    @Transactional(readOnly = true)
    public List<LicenseSuspensionDto> getSuspensionHistory(UUID driverId) {
        findEntityById(driverId); // validates driver exists
        return suspensionMapper.toDtoList(
            suspensionRepository.findByDriverIdOrderByStartDateDesc(driverId));
    }

    /** Returns the point history for a driver — used by DriverController. */
    @Transactional(readOnly = true)
    public List<DriverPointHistoryDto> getPointHistory(UUID driverId) {
        findEntityById(driverId); // validates driver exists
        return pointHistoryMapper.toDtoList(
            pointHistoryRepository.findByDriverIdOrderByOccurredAtDesc(driverId));
    }

    // ── job-level operations ──────────────────────────────────────────────

    /**
     * Annual reset of all penalty points to 0.
     * Called by LicensePointResetJob on January 1st.
     * Writes a history entry for every driver who had non-zero points.
     */
    @Transactional
    public void resetAllPenaltyPoints() {
        List<DriverEntity> drivers = driverRepository.findAll().stream()
                .filter(d -> d.getPenaltyPoints() > 0)
                .toList();

        for (DriverEntity driver : drivers) {
            int before = driver.getPenaltyPoints();
            driver.setPenaltyPoints(0);
            driverRepository.save(driver);
            writePointHistory(driver, -before, before, 0, "ANNUAL_RESET", null);
        }

        // Bulk SQL update handles any drivers missed by the in-memory loop
        driverRepository.resetAllPenaltyPoints();
    }

    /**
     * Lifts all suspensions whose endDate has passed.
     * Called by LicensePointResetJob (or a dedicated job).
     */
    @Transactional
    public void liftExpiredSuspensions() {
        List<LicenseSuspensionEntity> expired =
            suspensionRepository.findExpiredActiveSuspensions(LocalDate.now());

        for (LicenseSuspensionEntity suspension : expired) {
            DriverEntity driver = suspension.getDriver();
            driver.setSuspended(false);
            driver.setSuspendedUntil(null);
            driverRepository.save(driver);

            suspension.setActive(false);
            suspension.setLiftedAt(LocalDate.now());
            suspensionRepository.save(suspension);
        }
    }

    // DriverService — unchanged, still the one new method needed regardless of approach
    @Transactional(readOnly = true)
    public UUID getDriverIdForUser(UUID userId) {
        return driverRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No driver profile linked to this user account"))
                .getId();
    }


    // ── private helpers ───────────────────────────────────────────────────

    private LicenseSuspensionEntity suspendInternal(DriverEntity driver,
                                                     String reason,
                                                     LocalDate endDate,
                                                     UUID violationId,
                                                     UserEntity officer) {
        driver.setSuspended(true);
        driver.setSuspendedUntil(endDate);
        driverRepository.save(driver);

        LicenseSuspensionEntity suspension = new LicenseSuspensionEntity();
        suspension.setDriver(driver);
        suspension.setReason(reason);
        suspension.setStartDate(LocalDate.now());
        suspension.setEndDate(endDate);
        suspension.setPointsAtTime(driver.getPenaltyPoints());
        suspension.setActive(true);
        suspension.setViolationId(violationId);
        suspension.setSuspendedBy(officer);
        return suspensionRepository.save(suspension);
    }

    private void writePointHistory(DriverEntity driver, int change,
                                    int before, int after,
                                    String reason, UUID violationId) {
        DriverPointHistoryEntity history = new DriverPointHistoryEntity();
        history.setDriver(driver);
        history.setChangeAmount(change);
        history.setPointsBefore(before);
        history.setPointsAfter(after);
        history.setReason(reason);
        history.setViolationId(violationId);
        history.setOccurredAt(LocalDateTime.now());
        pointHistoryRepository.save(history);
    }


}
