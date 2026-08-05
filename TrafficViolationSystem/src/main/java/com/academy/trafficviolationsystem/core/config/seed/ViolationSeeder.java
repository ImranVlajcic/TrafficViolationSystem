package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.camera.CameraEventEntity;
import com.academy.trafficviolationsystem.camera.CameraEventRepository;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.vehicle.VehicleEntity;
import com.academy.trafficviolationsystem.violation.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Seeds 100 violations: 40 automatic (consumed from CameraEventEntity rows,
 * marking them processed=true) + 60 manual (officer-recorded).
 *
 * Final status distribution across all 100 (fixed, then shuffled):
 *   PENDING 12 · CONFIRMED 55 · DISUPTED 8 · DISSMISED 10 · CLOSED 15
 * (DISUPTED / DISSMISED spelled exactly as the real ViolationStatus enum —
 * confirmed real, not typos introduced here.)
 *
 * NOTE: cameraId is intentionally left null on automatic violations — see
 * the type-mismatch flag (UUID column vs. CameraEntity's Long id) raised
 * separately.
 */
@Component
public class ViolationSeeder {

    private static final int AUTOMATIC_COUNT = 40;
    private static final int MANUAL_COUNT = 60;

    private static final ViolationType[] TYPES = ViolationType.values();

    private final ViolationRepository violationRepository;
    private final CameraEventRepository cameraEventRepository;

    public ViolationSeeder(ViolationRepository violationRepository,
                           CameraEventRepository cameraEventRepository) {
        this.violationRepository = violationRepository;
        this.cameraEventRepository = cameraEventRepository;
    }

    public List<ViolationEntity> seed(List<CameraEventEntity> cameraEvents,
                                      List<VehicleEntity> vehicles,
                                      List<DriverEntity> drivers,
                                      List<UserEntity> officers) {
        if (violationRepository.count() > 0) {
            return violationRepository.findAll();
        }

        List<ViolationStatus> statusPlan = buildStatusPlan();
        Collections.shuffle(statusPlan, SeedRandom.RNG);

        List<ViolationEntity> violations = new ArrayList<>();
        int refSeq = 1;
        int year = java.time.LocalDate.now().getYear();

        // ── automatic violations, consuming camera events ──────────────────
        for (int i = 0; i < AUTOMATIC_COUNT && i < cameraEvents.size(); i++) {
            CameraEventEntity event = cameraEvents.get(i);
            VehicleEntity vehicle = findVehicleByPlate(vehicles, event.getLicensePlate());
            if (vehicle == null) vehicle = SeedRandom.pick(vehicles);

            ViolationStatus status = statusPlan.get(violations.size());
            ViolationType type = event.getMeasuredSpeed() != null && SeedRandom.chance(0.7)
                    ? ViolationType.SPEEDING
                    : SeedRandom.pick(TYPES);

            ViolationEntity violation = new ViolationEntity();
            violation.setReferenceNumber(String.format("TRF-%d-%06d", year, refSeq++));
            violation.setViolationType(type);
            violation.setDetectionMethod(SeedRandom.chance(0.5)
                    ? DetectionMethod.CAMERA_AUTO : DetectionMethod.RADAR_AUTO);
            violation.setStatus(status);
            violation.setOccurredAt(event.getReceivedAt());
            violation.setLocationLatitude(event.getEventLatitude() != null
                    ? event.getEventLatitude() : SeedRandom.RNG.nextDouble() * 0.1 + 43.8);
            violation.setLocationLongitude(event.getEventLongitude() != null
                    ? event.getEventLongitude() : SeedRandom.RNG.nextDouble() * 0.1 + 18.3);
            violation.setLocationDescription(SeedRandom.pick(SeedConstants.STREETS));

            if (type == ViolationType.SPEEDING) {
                int limit = SeedRandom.intBetween(30, 100);
                violation.setSpeedLimit(limit);
                violation.setMeasuredSpeed(limit + SeedRandom.intBetween(11, 60));
            }

            violation.setEvidenceImageUrl(event.getImageUrl());
            violation.setAutomatic(true);
            violation.setCameraId(null); // see type-mismatch note in CameraSeeder/ViolationEntity
            violation.setVehicle(vehicle);
            violation.setDriver(SeedRandom.chance(0.85) ? vehicle.getOwner() : null);
            violation.setOfficer(null);

            applyReviewIfNeeded(violation, status, officers);

            ViolationEntity saved = violationRepository.save(violation);
            violations.add(saved);

            event.setProcessed(true);
            event.setViolationId(saved.getId());
            cameraEventRepository.save(event);
        }

        // ── manual violations ───────────────────────────────────────────────
        for (int i = 0; i < MANUAL_COUNT; i++) {
            VehicleEntity vehicle = SeedRandom.pick(vehicles);
            ViolationStatus status = statusPlan.get(violations.size());
            ViolationType type = SeedRandom.pick(TYPES);

            ViolationEntity violation = new ViolationEntity();
            violation.setReferenceNumber(String.format("TRF-%d-%06d", year, refSeq++));
            violation.setViolationType(type);
            violation.setDetectionMethod(DetectionMethod.MANUAL_OFFICER);
            violation.setStatus(status == ViolationStatus.PENDING ? ViolationStatus.CONFIRMED : status);
            violation.setOccurredAt(SeedRandom.pastDateTime(1, 180));
            violation.setLocationLatitude(SeedRandom.RNG.nextDouble() * 0.1 + 43.8);
            violation.setLocationLongitude(SeedRandom.RNG.nextDouble() * 0.1 + 18.3);
            violation.setLocationDescription(SeedRandom.pick(SeedConstants.STREETS));

            if (type == ViolationType.SPEEDING) {
                int limit = SeedRandom.intBetween(30, 100);
                violation.setSpeedLimit(limit);
                violation.setMeasuredSpeed(limit + SeedRandom.intBetween(11, 60));
            }

            violation.setAutomatic(false);
            violation.setVehicle(vehicle);
            violation.setDriver(vehicle.getOwner());
            violation.setOfficer(SeedRandom.pick(officers));

            applyReviewIfNeeded(violation, violation.getStatus(), officers);

            violations.add(violationRepository.save(violation));
        }

        return violations;
    }

    private void applyReviewIfNeeded(ViolationEntity violation, ViolationStatus status,
                                     List<UserEntity> officers) {
        if (status != ViolationStatus.PENDING) {
            violation.setReviewedBy(SeedRandom.pick(officers));
            violation.setReviewedAt(violation.getOccurredAt().plusHours(SeedRandom.intBetween(1, 72)));
        }
    }

    private List<ViolationStatus> buildStatusPlan() {
        List<ViolationStatus> plan = new ArrayList<>();
        addN(plan, ViolationStatus.PENDING, 12);
        addN(plan, ViolationStatus.CONFIRMED, 55);
        addN(plan, ViolationStatus.DISPUTED, 8);
        addN(plan, ViolationStatus.DISMISSED, 10);
        addN(plan, ViolationStatus.CLOSED, 15);
        return plan;
    }

    private void addN(List<ViolationStatus> list, ViolationStatus status, int n) {
        for (int i = 0; i < n; i++) list.add(status);
    }

    private VehicleEntity findVehicleByPlate(List<VehicleEntity> vehicles, String plate) {
        if (plate == null) return null;
        for (VehicleEntity vehicle : vehicles) {
            if (vehicle.getLicensePlate().equals(plate)) return vehicle;
        }
        return null;
    }
}