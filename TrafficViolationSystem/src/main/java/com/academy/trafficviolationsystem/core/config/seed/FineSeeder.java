package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverPointHistoryEntity;
import com.academy.trafficviolationsystem.driver.DriverPointHistoryRepository;
import com.academy.trafficviolationsystem.driver.DriverRepository;
import com.academy.trafficviolationsystem.driver.LicenseSuspensionEntity;
import com.academy.trafficviolationsystem.driver.LicenseSuspensionRepository;
import com.academy.trafficviolationsystem.fine.FineEntity;
import com.academy.trafficviolationsystem.fine.FineRepository;
import com.academy.trafficviolationsystem.fine.FineStatus;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.violation.FineRuleEntity;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import com.academy.trafficviolationsystem.violation.ViolationRepository;
import com.academy.trafficviolationsystem.violation.ViolationStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds fines for CONFIRMED / DISUPTED / CLOSED violations, plus 6 of the 10
 * DISSMISED violations (representing "fine issued then cancelled on appeal" —
 * AppealSeeder later attaches a matching APPROVED appeal to these). The other
 * 4 DISSMISED violations get no fine at all (dismissed at review, before a
 * fine was ever issued).
 *
 * As a side effect, walks fines in issuedAt order per driver to build
 * DriverPointHistoryEntity rows and trigger LicenseSuspensionEntity rows
 * once a driver crosses the configured point threshold (12).
 */
@Component
public class FineSeeder {

    private static final int SUSPENSION_THRESHOLD = 12;
    private static final int DISSMISED_WITH_FINE_COUNT = 6;

    private final FineRepository fineRepository;
    private final ViolationRepository violationRepository;
    private final DriverRepository driverRepository;
    private final DriverPointHistoryRepository pointHistoryRepository;
    private final LicenseSuspensionRepository suspensionRepository;

    public FineSeeder(FineRepository fineRepository,
                      ViolationRepository violationRepository,
                      DriverRepository driverRepository,
                      DriverPointHistoryRepository pointHistoryRepository,
                      LicenseSuspensionRepository suspensionRepository) {
        this.fineRepository = fineRepository;
        this.violationRepository = violationRepository;
        this.driverRepository = driverRepository;
        this.pointHistoryRepository = pointHistoryRepository;
        this.suspensionRepository = suspensionRepository;
    }

    public static class FineSeedResult {
        public List<FineEntity> fines;
        public List<LicenseSuspensionEntity> suspensions;
    }

    public FineSeedResult seed(List<ViolationEntity> violations,
                               List<FineRuleEntity> fineRules,
                               List<UserEntity> officers) {
        FineSeedResult result = new FineSeedResult();

        if (fineRepository.count() > 0) {
            result.fines = fineRepository.findAll();
            result.suspensions = suspensionRepository.findAll();
            return result;
        }

        Map<String, FineRuleEntity> ruleByType = new HashMap<>();
        for (FineRuleEntity rule : fineRules) {
            ruleByType.put(rule.getViolationType().name(), rule);
        }

        List<ViolationEntity> dissmisedWithFine = new ArrayList<>();
        List<ViolationEntity> dissmisedShuffled = new ArrayList<>();
        for (ViolationEntity v : violations) {
            if (v.getStatus() == ViolationStatus.DISMISSED) dissmisedShuffled.add(v);
        }
        Collections.shuffle(dissmisedShuffled, SeedRandom.RNG);
        for (int i = 0; i < DISSMISED_WITH_FINE_COUNT && i < dissmisedShuffled.size(); i++) {
            dissmisedWithFine.add(dissmisedShuffled.get(i));
        }

        List<FineEntity> fines = new ArrayList<>();
        int seq = 1;
        int year = LocalDate.now().getYear();

        for (ViolationEntity violation : violations) {
            ViolationStatus vs = violation.getStatus();
            boolean shouldFine = vs == ViolationStatus.CONFIRMED
                    || vs == ViolationStatus.DISPUTED
                    || vs == ViolationStatus.CLOSED
                    || dissmisedWithFine.contains(violation);
            if (!shouldFine) continue;

            FineRuleEntity rule = ruleByType.get(violation.getViolationType().name());
            if (rule == null) continue;

            FineStatus targetStatus = decideTargetStatus(vs, dissmisedWithFine.contains(violation));

            LocalDateTime baseDate = violation.getReviewedAt() != null
                    ? violation.getReviewedAt() : violation.getOccurredAt();
            LocalDateTime issuedAt = baseDate.plusDays(SeedRandom.intBetween(1, 5));
            if (issuedAt.isAfter(LocalDateTime.now())) issuedAt = LocalDateTime.now().minusDays(1);
            LocalDate dueDate = issuedAt.toLocalDate().plusDays(rule.getPaymentDueDays());

            // OVERDUE requires the due date to have already passed; fall back to UNPAID otherwise.
            if (targetStatus == FineStatus.OVERDUE && !dueDate.isBefore(LocalDate.now())) {
                targetStatus = FineStatus.UNPAID;
            }

            FineEntity fine = new FineEntity();
            fine.setFineNumber(String.format("FIN-%d-%06d", year, seq++));
            fine.setAmount(rule.getBaseAmount());
            fine.setCurrency(SeedConstants.DEFAULT_CURRENCY);
            fine.setPenaltyPoints(rule.getPenaltyPoints());
            fine.setPaymentDueDays(rule.getPaymentDueDays());
            fine.setEarlyPayDiscountPct(rule.getEarlyPayDiscountPct());
            fine.setEarlyPayWindowDays(rule.getEarlyPayWindowDays());
            fine.setLateSurchargePct(rule.getLateSurchargePct());
            fine.setIssuedAt(issuedAt);
            fine.setDueDate(dueDate);
            fine.setViolationId(violation.getId());
            fine.setDriver(violation.getDriver() != null ? violation.getDriver() : violation.getVehicle().getOwner());
            fine.setIssuedBy(violation.isAutomatic() ? null : violation.getOfficer());

            applyAmountsForStatus(fine, rule, targetStatus, issuedAt);
            fine.setStatus(targetStatus);

            FineEntity saved = fineRepository.save(fine);
            fines.add(saved);

            violation.setFineId(saved.getId());
            violationRepository.save(violation);
        }

        List<LicenseSuspensionEntity> suspensions = applyDriverPointsAndSuspensions(fines, violations, officers);

        result.fines = fines;
        result.suspensions = suspensions;
        return result;
    }

    private FineStatus decideTargetStatus(ViolationStatus vs, boolean isDissmisedWithFine) {
        if (isDissmisedWithFine) return FineStatus.CANCELLED;
        if (vs == ViolationStatus.DISPUTED) return FineStatus.DISPUTED;
        if (vs == ViolationStatus.CLOSED) return FineStatus.PAID;
        // CONFIRMED: weighted mix
        double r = SeedRandom.RNG.nextDouble();
        if (r < 0.55) return FineStatus.UNPAID;
        if (r < 0.80) return FineStatus.OVERDUE;
        return FineStatus.PAID;
    }

    private void applyAmountsForStatus(FineEntity fine, FineRuleEntity rule,
                                       FineStatus status, LocalDateTime issuedAt) {
        BigDecimal amount = fine.getAmount();
        switch (status) {
            case PAID -> {
                boolean earlyPay = SeedRandom.chance(0.3);
                BigDecimal discount = earlyPay
                        ? amount.multiply(rule.getEarlyPayDiscountPct())
                        : BigDecimal.ZERO;
                fine.setDiscountAmount(discount);
                fine.setSurchargeAmount(BigDecimal.ZERO);
                fine.setTotalDue(amount.subtract(discount));
                fine.setPaidAt(issuedAt.plusDays(SeedRandom.intBetween(0, rule.getPaymentDueDays())));
            }
            case OVERDUE -> {
                BigDecimal surcharge = amount.multiply(rule.getLateSurchargePct());
                fine.setDiscountAmount(BigDecimal.ZERO);
                fine.setSurchargeAmount(surcharge);
                fine.setTotalDue(amount.add(surcharge));
            }
            default -> { // UNPAID, DISPUTED, CANCELLED
                fine.setDiscountAmount(BigDecimal.ZERO);
                fine.setSurchargeAmount(BigDecimal.ZERO);
                fine.setTotalDue(amount);
            }
        }
    }

    private List<LicenseSuspensionEntity> applyDriverPointsAndSuspensions(List<FineEntity> fines,
                                                                          List<ViolationEntity> violations,
                                                                          List<UserEntity> officers) {
        List<LicenseSuspensionEntity> suspensions = new ArrayList<>();
        Map<UUID, ViolationEntity> violationById = new HashMap<>();
        for (ViolationEntity v : violations) violationById.put(v.getId(), v);

        List<FineEntity> pointBearing = new ArrayList<>();
        for (FineEntity f : fines) {
            if (f.getStatus() != FineStatus.CANCELLED) pointBearing.add(f);
        }
        pointBearing.sort((a, b) -> a.getIssuedAt().compareTo(b.getIssuedAt()));

        Map<UUID, Integer> runningPoints = new HashMap<>();
        Map<UUID, DriverEntity> touchedDrivers = new HashMap<>();
        Map<UUID, Boolean> suspendedDrivers = new HashMap<>();

        for (FineEntity fine : pointBearing) {
            DriverEntity driver = fine.getDriver();
            if (driver == null) continue;
            UUID driverId = driver.getId();
            int before = runningPoints.getOrDefault(driverId, 0);
            int after = before + fine.getPenaltyPoints();
            runningPoints.put(driverId, after);
            touchedDrivers.putIfAbsent(driverId, driver);

            ViolationEntity violation = violationById.get(fine.getViolationId());

            DriverPointHistoryEntity history = new DriverPointHistoryEntity();
            history.setDriver(driver);
            history.setChangeAmount(fine.getPenaltyPoints());
            history.setPointsBefore(before);
            history.setPointsAfter(after);
            history.setReason("VIOLATION " + (violation != null ? violation.getReferenceNumber() : fine.getFineNumber()));
            history.setViolationId(fine.getViolationId());
            history.setOccurredAt(fine.getIssuedAt());
            pointHistoryRepository.save(history);

            if (after >= SUSPENSION_THRESHOLD && !Boolean.TRUE.equals(suspendedDrivers.get(driverId))) {
                suspendedDrivers.put(driverId, true);
                boolean stillActive = SeedRandom.chance(0.5);
                LocalDate startDate = fine.getIssuedAt().toLocalDate().plusDays(1);
                LocalDate endDate = startDate.plusMonths(SeedRandom.intBetween(3, 12));

                LicenseSuspensionEntity suspension = new LicenseSuspensionEntity();
                suspension.setDriver(driver);
                suspension.setReason("Prekoračen prag od " + SUSPENSION_THRESHOLD + " kaznenih poena.");
                suspension.setStartDate(startDate);
                suspension.setEndDate(endDate);
                suspension.setPointsAtTime(after);
                suspension.setViolationId(fine.getViolationId());
                suspension.setSuspendedBy(SeedRandom.pick(officers));

                if (stillActive && endDate.isAfter(LocalDate.now())) {
                    suspension.setActive(true);
                    driver.setSuspended(true);
                    driver.setSuspendedUntil(endDate);
                } else {
                    suspension.setActive(false);
                    suspension.setLiftedAt(endDate.isBefore(LocalDate.now()) ? endDate : LocalDate.now());
                    driver.setSuspended(false);
                    driver.setSuspendedUntil(null);
                }
                suspensionRepository.save(suspension);
                suspensions.add(suspension);
            }
        }

        for (Map.Entry<UUID, DriverEntity> entry : touchedDrivers.entrySet()) {
            DriverEntity driver = entry.getValue();
            driver.setPenaltyPoints(runningPoints.getOrDefault(entry.getKey(), 0));
            driverRepository.save(driver);
        }

        return suspensions;
    }
}