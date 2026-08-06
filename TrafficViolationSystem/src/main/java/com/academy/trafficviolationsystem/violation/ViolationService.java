package com.academy.trafficviolationsystem.violation;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.violations.ViolationAlreadyConfirmedException;
import com.academy.trafficviolationsystem.core.exceptions.violations.ViolationAlreadyDismissedException;
import com.academy.trafficviolationsystem.core.exceptions.violations.ViolationClosedException;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import com.academy.trafficviolationsystem.core.services.BaseCRUDService;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverRepository;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.user.UserRepository;
import com.academy.trafficviolationsystem.violation.insert.ReferenceNumberGenerator;
import com.academy.trafficviolationsystem.violation.insert.ViolationInsertChain;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core business logic for the violation domain.
 *
 * Implements BaseCRUDService — insert(), update(), search(), findById()
 * are handled by the base. Domain-specific operations are below.
 *
 * Key operations beyond CRUD:
 *   confirm(id, request, principal)  — officer confirms the violation as valid;
 *                                      publishes ViolationConfirmedEvent so
 *                                      FineService can listen and auto-issue a fine.
 *   dismiss(id, request, principal)  — officer dismisses as invalid/false positive.
 *   assignDriver(id, driverId)        — officer assigns the driver post-creation.
 *   closeViolation(id)               — called by FineService when fine is paid.
 *   generateReferenceNumber()         — TRF-{YEAR}-{6-digit-seq} format.
 *
 * beforeInsert() hydration/validation is a Chain of Responsibility —
 * see violation.insert.ViolationInsertChain and its handlers.
 *
 * Cross-module communication:
 *   This service publishes Spring ApplicationEvents rather than calling
 *   FineService directly. This keeps violation/ independent of fine/:
 *     ViolationConfirmedEvent → FineService creates the fine
 *   FineService calls ViolationRepository.setFineId() and .updateStatus()
 *   directly after the fine is saved — no callback needed here.
 */
@Service
@Transactional
public class ViolationService implements BaseCRUDService<
        ViolationEntity, ViolationDto, ViolationSearchObject, ViolationCreateRequest, ViolationUpdateRequest, UUID> {

    private final ViolationRepository      violationRepository;
    private final DriverRepository         driverRepository;
    private final UserRepository           userRepository;
    private final ViolationMapper          violationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager            entityManager;
    private final ViolationInsertChain     violationInsertChain;
    private final ReferenceNumberGenerator referenceNumberGenerator;

    public ViolationService(ViolationRepository violationRepository,
                            DriverRepository driverRepository,
                            UserRepository userRepository,
                            ViolationMapper violationMapper,
                            ApplicationEventPublisher eventPublisher,
                            EntityManager entityManager,
                            ViolationInsertChain violationInsertChain,
                            ReferenceNumberGenerator referenceNumberGenerator) {
        this.violationRepository = violationRepository;
        this.driverRepository    = driverRepository;
        this.userRepository      = userRepository;
        this.violationMapper     = violationMapper;
        this.eventPublisher      = eventPublisher;
        this.entityManager       = entityManager;
        this.violationInsertChain     = violationInsertChain;
        this.referenceNumberGenerator = referenceNumberGenerator;
    }

    // ── BaseCRUDService wiring ────────────────────────────────────────────

    @Override public ViolationRepository    getRepository()    { return violationRepository; }
    @Override public EntityManager          getEntityManager() { return entityManager;       }
    @Override public ViolationMapper        getMapper()        { return violationMapper;      }
    @Override public Class<ViolationEntity> getEntityClass()   { return ViolationEntity.class; }

    // ── lifecycle hooks ───────────────────────────────────────────────────

    @Override
    @AuditAction(value = "CREATE_VIOLATION", entityClass = ViolationEntity.class)
    public ViolationDto insert(ViolationCreateRequest request) {
        return BaseCRUDService.super.insert(request);
    }

    @Override
    @AuditAction(value = "UPDATE_VIOLATION", entityClass = ViolationEntity.class)
    public ViolationDto update(UUID id, ViolationUpdateRequest request) {
        return BaseCRUDService.super.update(id, request);
    }

    /**
     * Called by BaseCRUDService.insert() after mapping, before save.
     *
     * Delegates to the ViolationInsertChain (Chain of Responsibility),
     * which runs, in order: reference number generation, vehicle
     * load+validation, optional driver load, SPEEDING data validation,
     * then isAutomatic/status derivation. See ViolationInsertChain for
     * the wiring and each *Handler class for the individual steps —
     * this used to be one method doing all five things inline.
     */
    @Override
    public void beforeInsert(ViolationCreateRequest request, ViolationEntity entity) {
        violationInsertChain.handle(request, entity);
    }

    /**
     * Called after save. Publishes ViolationConfirmedEvent if the violation
     * was manually created (already CONFIRMED) so FineService issues the fine.
     */
    @Override
    public void afterInsert(ViolationCreateRequest request, ViolationEntity entity) {
        if (entity.getStatus() == ViolationStatus.CONFIRMED) {
            eventPublisher.publishEvent(new ViolationConfirmedEvent(this, entity));
        }
    }

    /**
     * Called before update. Loads the driver entity if driverId is being assigned.
     */
    @Override
    public void beforeUpdate(ViolationUpdateRequest request, ViolationEntity entity) {
        if (entity.getStatus() == ViolationStatus.DISMISSED) {
            throw new ViolationAlreadyDismissedException(entity.getReferenceNumber());
        }
        if (entity.getStatus() == ViolationStatus.CLOSED) {
            throw new ViolationClosedException(entity.getReferenceNumber());
        }

        if (request.getDriverId() != null) {
            DriverEntity driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new NotFoundException("Driver " + request.getDriverId() + " not found"));
            entity.setDriver(driver);
        }

        if (request.getMeasuredSpeed() != null && request.getSpeedLimit() != null) {
            if (entity.getViolationType() == ViolationType.SPEEDING
                    && request.getMeasuredSpeed() <= request.getSpeedLimit()) {
                throw new BadRequestException(
                        "measuredSpeed must exceed speedLimit for a SPEEDING violation");
            }
        }
    }

    // ── search filters ────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                            ViolationSearchObject searchObj,
                                            Root<ViolationEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), searchObj.getStatus()));
        }
        if (searchObj.getViolationType() != null) {
            predicates.add(cb.equal(root.get("violationType"), searchObj.getViolationType()));
        }
        if (searchObj.getDetectionMethod() != null) {
            predicates.add(cb.equal(root.get("detectionMethod"), searchObj.getDetectionMethod()));
        }
        if (searchObj.getVehicleId() != null) {
            predicates.add(cb.equal(root.get("vehicle").get("id"), searchObj.getVehicleId()));
        }
        if (searchObj.getDriverId() != null) {
            predicates.add(cb.equal(root.get("driver").get("id"), searchObj.getDriverId()));
        }
        if (searchObj.getOfficerId() != null) {
            predicates.add(cb.equal(root.get("officer").get("id"), searchObj.getOfficerId()));
        }
        if (searchObj.getCameraId() != null) {
            predicates.add(cb.equal(root.get("cameraId"), searchObj.getCameraId()));
        }
        if (searchObj.getIsAutomatic() != null) {
            predicates.add(cb.equal(root.get("isAutomatic"), searchObj.getIsAutomatic()));
        }
        if (searchObj.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    root.get("occurredAt"), searchObj.getFromDate().atStartOfDay()));
        }
        if (searchObj.getToDate() != null) {
            predicates.add(cb.lessThan(
                    root.get("occurredAt"), searchObj.getToDate().plusDays(1).atStartOfDay()));
        }
        if (searchObj.getSearch() != null && !searchObj.getSearch().isBlank()) {
            String pattern = "%" + searchObj.getSearch().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("referenceNumber")),      pattern),
                    cb.like(cb.lower(root.get("locationDescription")),  pattern),
                    cb.like(cb.lower(root.get("notes")),                pattern)
            ));
        }
        return predicates;
    }

    // ── review operations ─────────────────────────────────────────────────

    /**
     * Officer confirms a PENDING automatic violation as valid.
     * Publishes ViolationConfirmedEvent — FineService listens and creates the fine.
     *
     * Transition: PENDING → CONFIRMED
     */
    @Transactional
    @AuditAction(value = "CONFIRM_VIOLATION", entityClass = ViolationEntity.class)
    public ViolationDto confirm(UUID violationId, ReviewViolationRequest request,
                                UserPrincipal principal) {
        ViolationEntity violation = findEntityById(violationId);

        if (violation.getStatus() != ViolationStatus.PENDING) {
            throw new ViolationAlreadyConfirmedException(
                    "Violation " + violation.getReferenceNumber() + " is not in PENDING status (current: "
                            + violation.getStatus() + ")");
        }
        if (violation.getDriver() == null) {
            throw new BadRequestException(
                    "A driver must be assigned before confirming violation " +
                            violation.getReferenceNumber());
        }

        UserEntity reviewer = loadUser(principal.getId());
        violation.setStatus(ViolationStatus.CONFIRMED);
        violation.setReviewedBy(reviewer);
        violation.setReviewedAt(LocalDateTime.now());
        violation.setNotes(appendReviewNote(violation.getNotes(), "CONFIRMED", request.getReviewNotes(), reviewer));
        violationRepository.save(violation);

        eventPublisher.publishEvent(new ViolationConfirmedEvent(this, violation));
        return violationMapper.toDto(violation);
    }

    /**
     * Officer dismisses a PENDING violation as invalid.
     * No fine is issued. No penalty points applied.
     *
     * Transition: PENDING → DISMISSED
     */
    @Transactional
    @AuditAction(value = "DISMISS_VIOLATION", entityClass = ViolationEntity.class)
    public ViolationDto dismiss(UUID violationId, ReviewViolationRequest request,
                                UserPrincipal principal) {
        ViolationEntity violation = findEntityById(violationId);

        if (violation.getStatus() != ViolationStatus.PENDING) {
            throw new BadRequestException(
                    "Only PENDING violations can be dismissed. Current status: "
                            + violation.getStatus());
        }

        UserEntity reviewer = loadUser(principal.getId());

        violation.setStatus(ViolationStatus.DISMISSED);
        violation.setReviewedBy(reviewer);
        violation.setReviewedAt(LocalDateTime.now());
        violation.setNotes(
                appendReviewNote(
                        violation.getNotes(),
                        "DISMISSED",
                        request.getReviewNotes(),
                        reviewer));

        violationRepository.save(violation);

        return violationMapper.toDto(violation);
    }

    /**
     * Marks the violation as DISPUTED when a driver files an appeal.
     * Called by AppealService — not a direct HTTP endpoint on this controller.
     *
     * Transition: CONFIRMED → DISPUTED
     */
    @Transactional
    @AuditAction(value = "DISPUTE_VIOLATION", entityClass = ViolationEntity.class, captureSnapshot = false)
    public void markDisputed(UUID violationId) {
        ViolationEntity violation = findEntityById(violationId);
        if (violation.getStatus() != ViolationStatus.CONFIRMED) {
            throw new AppException(HttpStatus.CONFLICT, ErrorCode.BAD_REQUEST,
                    "Only CONFIRMED violations can be disputed");
        }
        violationRepository.updateStatus(violationId, ViolationStatus.DISPUTED);
    }

    /**
     * Re-confirms a DISPUTED violation after an appeal is rejected.
     * Called by AppealService.
     *
     * Transition: DISPUTED → CONFIRMED
     */
    @Transactional
    @AuditAction(value = "REINSTATE_VIOLATION", entityClass = ViolationEntity.class, captureSnapshot = false)
    public void reinstateAfterAppealRejection(UUID violationId) {
        violationRepository.updateStatus(violationId, ViolationStatus.CONFIRMED);
    }

    /**
     * Closes the violation after the fine is paid.
     * Called by FineService — not a direct HTTP endpoint.
     *
     * Transition: CONFIRMED → CLOSED
     */
    @Transactional
    @AuditAction(value = "CLOSE_VIOLATION", entityClass = ViolationEntity.class, captureSnapshot = false)
    public void closeViolation(UUID violationId) {
        violationRepository.updateStatus(violationId, ViolationStatus.CLOSED);
    }

    /**
     * Back-links the fine ID onto the violation after FineService creates it.
     * Called by FineService.afterInsert().
     */
    @Transactional
    @AuditAction(value = "LINK_FINE_TO_VIOLATION", entityClass = ViolationEntity.class, captureSnapshot = false)
    public void linkFine(UUID violationId, UUID fineId) {
        violationRepository.setFineId(violationId, fineId);
    }

    // ── reference number ──────────────────────────────────────────────────

    /**
     * Generates the next reference number in the format TRF-{YEAR}-{6-digit-seq}.
     * e.g. TRF-2025-000001, TRF-2025-000002, ...
     *
     * Public API preserved for existing callers; the actual year/count
     * logic now lives in ReferenceNumberGenerator, shared with
     * ReferenceNumberHandler in the insert chain.
     */
    public String generateReferenceNumber() {
        return referenceNumberGenerator.generate();
    }

    // ── read helpers (used by other modules) ──────────────────────────────

    /**
     * Returns all violations for a driver, newest first.
     * Used by DriverController GET /api/drivers/{id}/violations
     * and by the citizen portal to scope data to the logged-in driver.
     */
    @Transactional(readOnly = true)
    public List<ViolationDto> getViolationsForDriver(UUID driverId) {
        return violationMapper.toDtoList(
                violationRepository.findByDriverIdOrderByOccurredAtDesc(driverId));
    }

    /**
     * Returns all violations for a vehicle, newest first.
     * Used by VehicleController GET /api/vehicles/{id}/violations.
     */
    @Transactional(readOnly = true)
    public List<ViolationDto> getViolationsForVehicle(UUID vehicleId) {
        return violationMapper.toDtoList(
                violationRepository.findByVehicleIdOrderByOccurredAtDesc(vehicleId));
    }

    // ── private helpers ───────────────────────────────────────────────────

    private UserEntity loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
    }

    private String appendReviewNote(String existing, String action,
                                    String reviewNote, UserEntity reviewer) {
        String entry = String.format("[%s by %s at %s] %s",
                action,
                reviewer.getUsername(),
                LocalDateTime.now(),
                reviewNote);
        return existing == null || existing.isBlank() ? entry : existing + "\n" + entry;
    }
}