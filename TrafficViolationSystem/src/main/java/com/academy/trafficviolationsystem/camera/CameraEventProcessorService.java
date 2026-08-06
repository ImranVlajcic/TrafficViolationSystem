package com.academy.trafficviolationsystem.camera;

import com.academy.trafficviolationsystem.notification.NotificationService;
import com.academy.trafficviolationsystem.vehicle.VehicleEntity;
import com.academy.trafficviolationsystem.vehicle.VehicleRepository;
import com.academy.trafficviolationsystem.violation.DetectionMethod;
import com.academy.trafficviolationsystem.violation.ViolationCreateRequest;
import com.academy.trafficviolationsystem.violation.ViolationService;
import com.academy.trafficviolationsystem.violation.ViolationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Converts a raw CameraEventEntity into a ViolationEntity via ViolationService.
 *
 * Runs in REQUIRES_NEW propagation — its own transaction completely independent
 * from the MqttViolationListener transaction. This means:
 *   - If this processor fails, the CameraEventEntity row is already committed
 *     (written by the listener) and can be retried.
 *   - A processor failure does not roll back the raw event persist.
 *
 * Logic per camera type:
 *   SPEED_RADAR / MOBILE_RADAR → SPEEDING violation if measuredSpeed > speedLimit
 *   RED_LIGHT                  → RED_LIGHT violation
 *   ANPR / OVERHEAD            → no automatic violation (plate read only);
 *                                 checks stolen flag via VehicleService
 *
 * Stolen vehicle detection:
 *   On every plate read (regardless of camera type), if VehicleEntity.isStolen = true,
 *   a STOLEN_VEHICLE event is logged. This is separate from the speed/red-light violation.
 *   In this implementation we log a warning — a full implementation would publish
 *   an alert notification via NotificationService.
 */
@Service
public class CameraEventProcessorService {

    private static final Logger log = LoggerFactory.getLogger(CameraEventProcessorService.class);

    private final VehicleRepository     vehicleRepository;
    private final ViolationService      violationService;
    private final CameraEventRepository cameraEventRepository;
    private final NotificationService notificationService;

    public CameraEventProcessorService(VehicleRepository vehicleRepository,
                                       ViolationService violationService,
                                       CameraEventRepository cameraEventRepository, NotificationService notificationService) {
        this.vehicleRepository      = vehicleRepository;
        this.violationService       = violationService;
        this.cameraEventRepository  = cameraEventRepository;
        this.notificationService = notificationService;
    }

    /**
     * Main processing entry point called by MqttViolationListener.
     * Runs in its own transaction (REQUIRES_NEW).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(CameraEventEntity event,
                        CameraEntity camera,
                        MqttEventPayload payload) {

        // Guard against duplicate processing on retry — if a violation was already
        // created from this event (e.g. markProcessed failed after insert succeeded),
        // don't create a second one.
        if (event.getViolationId() != null) {
            log.warn("Event {} already linked to violation {} — skipping duplicate processing",
                    event.getId(), event.getViolationId());
            return;
        }

        // Cannot process without a plate
        if (payload.getPlate() == null || payload.getPlate().isBlank()) {
            log.warn("Event {} has no license plate — marking unprocessable", event.getId());
            cameraEventRepository.markFailed(event.getId(), "No license plate in payload");
            return;
        }

        // Look up vehicle by plate
        VehicleEntity vehicle = vehicleRepository
                .findByLicensePlateIgnoreCase(payload.getPlate().toUpperCase().trim())
                .orElse(null);

        if (vehicle == null) {
            String msg = "No vehicle registered for plate: " + payload.getPlate();
            log.warn("Event {} — {}", event.getId(), msg);
            cameraEventRepository.markFailed(event.getId(), msg);
            return;
        }

        // Stolen vehicle alert (non-blocking — log for now, wire NotificationService here later)
        if (vehicle.isStolen()) {
            log.warn("STOLEN VEHICLE DETECTED — plate: {}, camera: {}, event: {}",
                    payload.getPlate(), camera.getSerialNumber(), event.getId());
            notificationService.sendStolenVehicleAlert(vehicle, camera);
        }

        // Determine if a violation should be created
        ViolationCreateRequest request = buildViolationRequest(event, camera, payload, vehicle);

        if (request == null) {
            // No violation warranted (e.g. ANPR read with no infraction detected)
            cameraEventRepository.markProcessed(event.getId(), null);
            return;
        }

        // Create the violation — ViolationService.beforeInsert() does all the business logic
        var dto = violationService.insert(request);

        // Back-link the violation UUID onto the event row
        cameraEventRepository.markProcessed(event.getId(), dto.getId());

        log.info("Violation {} created from camera event {} (camera: {}, plate: {})",
            dto.getReferenceNumber(), event.getId(), camera.getSerialNumber(), payload.getPlate());
    }

    // ── private helpers ───────────────────────────────────────────────────

    /**
     * Decides whether this event warrants a violation and builds the request.
     * Returns null if no violation should be created.
     */
    private ViolationCreateRequest buildViolationRequest(CameraEventEntity event,
                                                          CameraEntity camera,
                                                          MqttEventPayload payload,
                                                          VehicleEntity vehicle) {
        ViolationType violationType   = resolveViolationType(camera, payload);
        DetectionMethod detectionMethod = resolveDetectionMethod(camera);

        if (violationType == null) {
            return null; // ANPR read with no infraction
        }

        ViolationCreateRequest req = new ViolationCreateRequest();
        req.setVehicleId(vehicle.getId());
        req.setViolationType(violationType);
        req.setDetectionMethod(detectionMethod);
        req.setOccurredAt(resolveTimestamp(payload, event));
        req.setCameraId((camera.getId()));

        // Location: prefer payload GPS, fall back to camera's registered position
        req.setLocationLatitude(payload.getLatitude() != null
                ? payload.getLatitude() : camera.getLatitude());
        req.setLocationLongitude(payload.getLongitude() != null
                ? payload.getLongitude() : camera.getLongitude());
        req.setLocationDescription(camera.getLocationDescription());

        // Speed data
        req.setMeasuredSpeed(payload.getMeasuredSpeedKmh());
        req.setSpeedLimit(resolveSpeedLimit(payload, camera));

        // Evidence
        req.setEvidenceImageUrl(payload.getImageUrl());
        req.setEvidenceVideoUrl(payload.getVideoUrl());

        // Driver is not known at detection time — officer assigns it during review
        req.setDriverId(vehicle.getOwner() != null ? vehicle.getOwner().getId() : null);

        return req;
    }

    /**
     * Determines the violation type based on camera type and payload content.
     * Returns null if the event is a plate read with no infraction (ANPR/OVERHEAD passing scan).
     */
    private ViolationType resolveViolationType(CameraEntity camera, MqttEventPayload payload) {
        return switch (camera.getCameraType()) {
            case SPEED_RADAR, MOBILE_RADAR -> {
                Integer measured = payload.getMeasuredSpeedKmh();
                Integer limit    = resolveSpeedLimit(payload, camera);
                if (measured != null && limit != null && measured > limit) {
                    yield ViolationType.SPEEDING;
                }
                yield null; // speed check passed
            }
            case RED_LIGHT -> ViolationType.RED_LIGHT;
            case ANPR, OVERHEAD -> null; // pure plate reads — no automatic violation
        };
    }

    private DetectionMethod resolveDetectionMethod(CameraEntity camera) {
        return switch (camera.getCameraType()) {
            case SPEED_RADAR -> DetectionMethod.RADAR_AUTO;
            case MOBILE_RADAR -> DetectionMethod.RADAR_AUTO;
            default -> DetectionMethod.CAMERA_AUTO;
        };
    }

    private Integer resolveSpeedLimit(MqttEventPayload payload, CameraEntity camera) {
        // Payload speed limit takes precedence; fall back to camera's configured limit
        return payload.getSpeedLimitKmh() != null
                ? payload.getSpeedLimitKmh()
                : camera.getSpeedLimitKmh();
    }

    private LocalDateTime resolveTimestamp(MqttEventPayload payload, CameraEventEntity event) {
        if (payload.getTimestampEpochMs() != null) {
            return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(payload.getTimestampEpochMs()),
                ZoneId.systemDefault()
            );
        }
        // Fall back to server receipt time
        return event.getReceivedAt();
    }
}
