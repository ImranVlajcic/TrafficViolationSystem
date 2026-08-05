package com.academy.trafficviolationsystem.fine;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.fine.FineAlreadyCancelledException;
import com.academy.trafficviolationsystem.core.exceptions.fine.FineAlreadyPaidException;
import com.academy.trafficviolationsystem.core.exceptions.fine.FineCancelledException;
import com.academy.trafficviolationsystem.core.exceptions.fine.FineNotDisputableException;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import com.academy.trafficviolationsystem.core.services.BaseService;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverRepository;
import com.academy.trafficviolationsystem.driver.DriverService;
import com.academy.trafficviolationsystem.violation.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for the fine domain.
 *
 * FineEntity is never created from an HTTP request body — it is created
 * exclusively in response to a ViolationConfirmedEvent published by
 * ViolationService. This enforces the rule that every fine must trace back
 * to a confirmed violation.
 *
 * Implements BaseService (not BaseCRUDService) because insert/update are not
 * exposed as generic CRUD operations. The fine creation is event-driven;
 * there is no PUT /api/fines/{id} for general updates.
 *
 * Key operations:
 *   onViolationConfirmed     — @EventListener, auto-creates FineEntity
 *   cancel(id, principal)    — ADMIN/OFFICER cancels a fine
 *   markDisputed(id)         — called by AppealService
 *   reinstateAfterRejection  — called by AppealService on appeal rejection
 *   markOverdueWithSurcharge — called by OverdueFineCheckerJob nightly
 *   getForDriver(driverId)   — citizen portal and driver detail page
 *
 * Cross-module writes:
 *   ViolationService.closeViolation() — closes violation when fine is paid
 *   DriverService.removePenaltyPoints() — reverses points on cancellation
 *
 * Fine issuance itself (points, suspension, notifications, back-link to
 * the violation) is orchestrated by ViolationWorkflowMediator, not here —
 * see issueFineForViolation().
 */
@Service
@Transactional
public class FineService implements BaseService<FineEntity, FineDto, FineSearchObject, UUID> {

    private final FineRepository       fineRepository;
    private final FineMapper           fineMapper;
    private final FinePdfService       finePdfService;
    private final FineRuleService      fineRuleService;
    private final DriverService        driverService;
    private final DriverRepository     driverRepository;
    private final ViolationRepository  violationRepository;
    private final ViolationService     violationService;
    private final EntityManager        entityManager;

    public FineService(FineRepository fineRepository,
                       FineMapper fineMapper,
                       FinePdfService finePdfService,
                       FineRuleService fineRuleService,
                       DriverService driverService,
                       DriverRepository driverRepository,
                       ViolationRepository violationRepository,
                       ViolationService violationService,
                       EntityManager entityManager) {
        this.fineRepository      = fineRepository;
        this.fineMapper          = fineMapper;
        this.finePdfService      = finePdfService;
        this.fineRuleService     = fineRuleService;
        this.driverService       = driverService;
        this.driverRepository    = driverRepository;
        this.violationRepository = violationRepository;
        this.violationService    = violationService;
        this.entityManager       = entityManager;
    }

    // ── BaseService wiring ────────────────────────────────────────────────

    @Override public CrudRepository<FineEntity, UUID> getRepository()    { return fineRepository;  }
    @Override public EntityManager                    getEntityManager() { return entityManager;   }
    @Override public FineMapper                       getMapper()        { return fineMapper;       }
    @Override public Class<FineEntity>                getEntityClass()   { return FineEntity.class; }

    // ── event-driven fine creation ────────────────────────────────────────

    /**
     * Builds and persists a FineEntity for a confirmed violation.
     * Pure fine-issuance — does not touch the driver or the violation's
     * own fields. Called exclusively by ViolationWorkflowMediator in
     * response to ViolationConfirmedEvent.
     *
     * @return empty if a fine already exists for this violation (guards
     *         against duplicate issuance if the event fires twice)
     */
    @Transactional
    @AuditAction(value = "ISSUE_FINE", entityClass = FineEntity.class)
    public Optional<FineEntity> issueFineForViolation(ViolationEntity violation) {
        if (fineRepository.existsByViolationId(violation.getId())) {
            return Optional.empty();
        }
        if (violation.getDriver() == null) {
            throw new BadRequestException(
                    "Cannot issue a fine for violation " + violation.getReferenceNumber() +
                            " — no driver is assigned.");
        }

        FineRuleEntity rule = fineRuleService.findActiveByType(violation.getViolationType());
        DriverEntity driver = driverRepository.findById(violation.getDriver().getId())
                .orElseThrow(() -> new NotFoundException(
                        "Driver " + violation.getDriver().getId() + " not found"));

        FineEntity fine = buildFine(violation, driver, rule);
        return Optional.of(fineRepository.save(fine));
    }

    // ── search ────────────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                            FineSearchObject searchObj,
                                            Root<FineEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), searchObj.getStatus()));
        }
        if (searchObj.getDriverId() != null) {
            predicates.add(cb.equal(root.get("driver").get("id"), searchObj.getDriverId()));
        }
        if (searchObj.getViolationId() != null) {
            predicates.add(cb.equal(root.get("violationId"), searchObj.getViolationId()));
        }
        if (searchObj.getIssuedById() != null) {
            predicates.add(cb.equal(root.get("issuedBy").get("id"), searchObj.getIssuedById()));
        }
        if (searchObj.getIssuedFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                root.get("issuedAt"), searchObj.getIssuedFrom().atStartOfDay()));
        }
        if (searchObj.getIssuedTo() != null) {
            predicates.add(cb.lessThan(
                root.get("issuedAt"), searchObj.getIssuedTo().plusDays(1).atStartOfDay()));
        }
        if (Boolean.TRUE.equals(searchObj.getOverdueDatePassed())) {
            predicates.add(cb.lessThan(root.get("dueDate"), LocalDate.now()));
            predicates.add(cb.notEqual(root.get("status"), FineStatus.PAID));
            predicates.add(cb.notEqual(root.get("status"), FineStatus.CANCELLED));
        }
        if (searchObj.getSearch() != null && !searchObj.getSearch().isBlank()) {
            predicates.add(cb.like(
                cb.lower(root.get("fineNumber")),
                "%" + searchObj.getSearch().toLowerCase() + "%"
            ));
        }
        return predicates;
    }

    // ── domain operations ─────────────────────────────────────────────────

    /**
     * Cancels a fine — called by officers/admins or triggered by appeal approval.
     *
     * Reverses penalty points applied at issuance.
     * If called from AppealService, reason should reference the appeal number.
     */
    @Transactional
    @AuditAction(value = "CANCEL_FINE", entityClass = FineEntity.class)
    public FineDto cancel(UUID fineId, String reason, UserPrincipal principal) {
        FineEntity fine = findEntityById(fineId);

        if (fine.getStatus() == FineStatus.PAID) {
            throw new FineAlreadyPaidException(fine.getFineNumber());
        }
        if (fine.getStatus() == FineStatus.CANCELLED) {
            throw new FineAlreadyCancelledException(fine.getFineNumber());
        }

        fine.setStatus(FineStatus.CANCELLED);
        fineRepository.save(fine);

        // Reverse penalty points
        driverService.removePenaltyPoints(
            fine.getDriver().getId(),
            fine.getPenaltyPoints(),
            "FINE_CANCELLED " + fine.getFineNumber() + (reason != null ? " — " + reason : "")
        );

        return fineMapper.toDto(fine);
    }

    /**
     * Marks a fine as DISPUTED when an appeal is filed.
     * Called by AppealService — not directly via HTTP.
     */
    @Transactional
    @AuditAction(value = "MARK_FINE_DISPUTED", entityClass = FineEntity.class)
    public void markDisputed(UUID fineId) {
        FineEntity fine = findEntityById(fineId);
        if (fine.getStatus() != FineStatus.UNPAID && fine.getStatus() != FineStatus.OVERDUE) {
            throw new FineNotDisputableException(fineId,fine.getStatus());
        }
        fineRepository.updateStatus(fineId, FineStatus.DISPUTED);
    }

    /**
     * Reinstates a DISPUTED fine back to UNPAID after an appeal is rejected.
     * Called by AppealService.
     */
    @Transactional
    @AuditAction(value = "REINSTATE_FINE", entityClass = FineEntity.class)
    public void reinstateAfterAppealRejection(UUID fineId) {
        fineRepository.updateStatus(fineId, FineStatus.UNPAID);
    }

    /**
     * Marks a fine as PAID and closes the associated violation.
     * Called by PaymentService after a successful payment.
     */
    @Transactional
    @AuditAction(value = "MARK_FINE_PAID", entityClass = FineEntity.class)
    public void markPaid(UUID fineId) {
        FineEntity fine = findEntityById(fineId);

        if (fine.getStatus() == FineStatus.PAID) {
            throw new FineAlreadyPaidException(fine.getFineNumber());
        }
        if (fine.getStatus() == FineStatus.CANCELLED) {
            throw new FineCancelledException(fine.getFineNumber());
        }

        FineAmountComponent amount = new BaseFineAmount(
                fine.getAmount(), BigDecimal.ZERO, fine.getSurchargeAmount());
        amount = new EarlyPaymentDiscountDecorator(
                amount, fine.getEarlyPayDiscountPct(),
                fine.getIssuedAt().toLocalDate(), fine.getEarlyPayWindowDays());

        fine.setDiscountAmount(amount.getDiscountAmount());
        fine.setTotalDue(amount.getTotalDue());
        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now());
        fineRepository.save(fine);

        violationService.closeViolation(fine.getViolationId());
    }

    /**
     * Applies surcharge and marks overdue. Called by OverdueFineCheckerJob.
     * Returns the number of fines processed.
     */
    @Transactional
    public int markOverdueWithSurcharge() {
        List<FineEntity> overdueFines = fineRepository.findUnpaidPassedDueDate(LocalDate.now());

        // in markOverdueWithSurcharge()
        for (FineEntity fine : overdueFines) {
            FineAmountComponent amount = new LateSurchargeDecorator(
                    new BaseFineAmount(fine.getAmount()), fine.getLateSurchargePct());

            fine.setSurchargeAmount(amount.getSurchargeAmount());
            fine.setTotalDue(amount.getTotalDue());
            fine.setStatus(FineStatus.OVERDUE);
            fineRepository.save(fine);
        }

        return overdueFines.size();
    }

    // ── read helpers ──────────────────────────────────────────────────────

    /**
     * All fines for a driver, newest first.
     * Populates violationReference by joining with ViolationRepository.
     */
    @Transactional(readOnly = true)
    public List<FineDto> getForDriver(UUID driverId) {
        List<FineEntity> fines = fineRepository.findByDriverIdOrderByIssuedAtDesc(driverId);
        return fines.stream()
                .map(this::toDtoWithViolationRef)
                .toList();
    }

    /**
     * Fetches a single fine and enriches the DTO with the violation reference number.
     */
    @Transactional(readOnly = true)
    public FineDto getFineWithDetails(UUID fineId) {
        FineEntity fine = findEntityById(fineId);
        return toDtoWithViolationRef(fine);
    }

    // ── private helpers ───────────────────────────────────────────────────

    private FineEntity buildFine(ViolationEntity violation,
                                  DriverEntity driver,
                                  FineRuleEntity rule) {
        LocalDateTime now = LocalDateTime.now();

        FineEntity fine = new FineEntity();
        fine.setFineNumber(generateFineNumber());
        fine.setViolationId(violation.getId());
        fine.setDriver(driver);
        fine.setStatus(FineStatus.UNPAID);
        fine.setIssuedAt(now);

        // Copy rule amounts as immutable snapshots
        fine.setAmount(rule.getBaseAmount());
        fine.setDiscountAmount(BigDecimal.ZERO);
        fine.setSurchargeAmount(BigDecimal.ZERO);
        fine.setTotalDue(rule.getBaseAmount());
        fine.setPenaltyPoints(rule.getPenaltyPoints());
        fine.setPaymentDueDays(rule.getPaymentDueDays());
        fine.setEarlyPayDiscountPct(rule.getEarlyPayDiscountPct());
        fine.setEarlyPayWindowDays(rule.getEarlyPayWindowDays());
        fine.setLateSurchargePct(rule.getLateSurchargePct());
        fine.setDueDate(now.toLocalDate().plusDays(rule.getPaymentDueDays()));

        return fine;
    }

    private FineDto toDtoWithViolationRef(FineEntity fine) {
        FineDto dto = fineMapper.toDto(fine);
        violationRepository.findById(fine.getViolationId())
                .ifPresent(v -> dto.setViolationReference(v.getReferenceNumber()));
        return dto;
    }

    private String generateFineNumber() {
        int year = LocalDate.now().getYear();
        LocalDateTime yearStart = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime yearEnd   = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        long count = fineRepository.countByYear(yearStart, yearEnd);
        return String.format("FIN-%d-%06d", year, count + 1);
    }
}
