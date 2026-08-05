package com.academy.trafficviolationsystem.camera;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.camera.ConflictException;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for camera and radar device management.
 *
 * Implements BaseCRUDService — search(), findById(), insert(), update()
 * are handled by the base.
 *
 * Extra operations:
 *   logMaintenance(id, request, principal) — POST /api/cameras/{id}/maintenance
 *   completeMaintenance(cameraId, logId)   — POST /api/cameras/{id}/maintenance/{logId}/complete
 *   getMaintenanceHistory(id)              — GET  /api/cameras/{id}/maintenance
 *   getEventHistory(id)                    — GET  /api/cameras/{id}/events
 *   retryFailedEvents()                    — called by CameraHeartbeatJob
 */
@Service
@Transactional
public class CameraService implements BaseCRUDService<
        CameraEntity, CameraDto, CameraSearchObject, CameraCreateRequest, CameraUpdateRequest, Integer> {

    private static final int MAX_RETRIES         = 3;
    private static final int OFFLINE_THRESHOLD_MIN = 10; // minutes without heartbeat → offline

    private final CameraRepository              cameraRepository;
    private final CameraEventRepository         cameraEventRepository;
    private final CameraMaintenanceLogRepository maintenanceRepository;
    private final UserRepository                userRepository;
    private final CameraMapper                  cameraMapper;
    private final CameraEventMapper             eventMapper;
    private final CameraMaintenanceLogMapper    maintenanceMapper;
    private final CameraEventProcessorService   processorService;
    private final EntityManager                 entityManager;

    public CameraService(CameraRepository cameraRepository,
                         CameraEventRepository cameraEventRepository,
                         CameraMaintenanceLogRepository maintenanceRepository,
                         UserRepository userRepository,
                         CameraMapper cameraMapper,
                         CameraEventMapper eventMapper,
                         CameraMaintenanceLogMapper maintenanceMapper,
                         CameraEventProcessorService processorService,
                         EntityManager entityManager) {
        this.cameraRepository    = cameraRepository;
        this.cameraEventRepository = cameraEventRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.userRepository      = userRepository;
        this.cameraMapper        = cameraMapper;
        this.eventMapper         = eventMapper;
        this.maintenanceMapper   = maintenanceMapper;
        this.processorService    = processorService;
        this.entityManager       = entityManager;
    }

    // ── BaseCRUDService wiring ────────────────────────────────────────────

    @Override public CameraRepository    getRepository()    { return cameraRepository; }
    @Override public EntityManager       getEntityManager() { return entityManager;    }
    @Override public CameraMapper        getMapper()        { return cameraMapper;     }
    @Override public Class<CameraEntity> getEntityClass()   { return CameraEntity.class; }

    // ── lifecycle hooks ───────────────────────────────────────────────────

    @Override
    @AuditAction(value = "REGISTER_CAMERA", entityClass = CameraEntity.class)
    public CameraDto insert(CameraCreateRequest request) {
        return BaseCRUDService.super.insert(request);
    }

    @Override
    @AuditAction(value = "UPDATE_CAMERA", entityClass = CameraEntity.class)
    public CameraDto update(Integer id, CameraUpdateRequest request) {
        return BaseCRUDService.super.update(id, request);
    }

    @Override
    public void beforeInsert(CameraCreateRequest request, CameraEntity entity) {
        if (cameraRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new BadRequestException(
                "Serial number '" + request.getSerialNumber() + "' is already registered");
        }
        if (cameraRepository.existsByMqttTopic(request.getMqttTopic())) {
            throw new BadRequestException(
                "MQTT topic '" + request.getMqttTopic() + "' is already in use by another camera");
        }
        // Enforce topic convention: cameras/{serial}/events
        String expectedTopic = "cameras/" + request.getSerialNumber() + "/events";
        if (!request.getMqttTopic().equals(expectedTopic)) {
            throw new BadRequestException(
                "MQTT topic must follow the convention: " + expectedTopic +
                " (received: " + request.getMqttTopic() + ")");
        }
    }

    @Override
    public void beforeUpdate(CameraUpdateRequest request, CameraEntity entity) {
        // Nothing extra needed — mapper guards immutable fields
    }

    // ── search ────────────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                            CameraSearchObject searchObj,
                                            Root<CameraEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getSearch() != null && !searchObj.getSearch().isBlank()) {
            String pattern = "%" + searchObj.getSearch().toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("name")),                pattern),
                cb.like(cb.lower(root.get("serialNumber")),        pattern),
                cb.like(cb.lower(root.get("locationDescription")), pattern)
            ));
        }
        if (searchObj.getCameraType() != null) {
            predicates.add(cb.equal(root.get("cameraType"), searchObj.getCameraType()));
        }
        if (searchObj.getIsOnline() != null) {
            predicates.add(cb.equal(root.get("isOnline"), searchObj.getIsOnline()));
        }
        if (searchObj.getIsActive() != null) {
            predicates.add(cb.equal(root.get("isActive"), searchObj.getIsActive()));
        }
        return predicates;
    }

    // ── maintenance operations ────────────────────────────────────────────

    /**
     * Logs a maintenance visit for a camera.
     * If the firmware version changed, updates CameraEntity.firmwareVersion.
     */
    @Transactional
    @AuditAction(value = "LOG_CAMERA_MAINTENANCE", entityClass = CameraMaintenanceLogEntity.class)
    public CameraMaintenanceLogDto logMaintenance(Integer cameraId,
                                                   LogMaintenanceRequest request,
                                                   UserPrincipal principal) {
        CameraEntity camera = findEntityById(cameraId);

        UserEntity technician = userRepository.findById(principal.getId()).orElse(null);

        CameraMaintenanceLogEntity log = new CameraMaintenanceLogEntity();
        log.setCamera(camera);
        log.setMaintenanceType(request.getMaintenanceType());
        log.setScheduledDate(request.getScheduledDate());
        log.setNotes(request.getNotes());
        log.setFirmwareBefore(request.getFirmwareBefore() != null
                ? request.getFirmwareBefore()
                : camera.getFirmwareVersion());
        log.setFirmwareAfter(request.getFirmwareAfter());
        log.setPerformedBy(technician);
        log.setCompleted(request.isCompleted());

        if (request.isCompleted()) {
            log.setCompletedAt(LocalDateTime.now());
        }

        // Update firmware version on the camera if it changed
        if (request.getFirmwareAfter() != null && !request.getFirmwareAfter().isBlank()) {
            camera.setFirmwareVersion(request.getFirmwareAfter());
            cameraRepository.save(camera);
        }

        return maintenanceMapper.toDto(maintenanceRepository.save(log));
    }

    /**
     * Marks a scheduled maintenance entry as completed.
     */
    @Transactional
    @AuditAction(value = "COMPLETE_CAMERA_MAINTENANCE", entityClass = CameraMaintenanceLogEntity.class)
    public CameraMaintenanceLogDto completeMaintenance(Integer cameraId, UUID logId,
                                                        UserPrincipal principal) {
        CameraMaintenanceLogEntity log = maintenanceRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("Maintenance log " + logId + " not found"));

        if (!log.getCamera().getId().equals(cameraId)) {
            throw new BadRequestException(
                    "Maintenance log does not belong to camera " + cameraId);
        }
        if (log.isCompleted()) {
            throw new ConflictException("Maintenance log is already marked as completed");
        }

        log.setCompleted(true);
        log.setCompletedAt(LocalDateTime.now());
        if (log.getPerformedBy() == null) {
            userRepository.findById(principal.getId()).ifPresent(log::setPerformedBy);
        }

        return maintenanceMapper.toDto(maintenanceRepository.save(log));
    }

    // ── read helpers ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CameraMaintenanceLogDto> getMaintenanceHistory(Integer cameraId) {
        findEntityById(cameraId);
        return maintenanceMapper.toDtoList(
            maintenanceRepository.findByCameraIdOrderByCompletedAtDesc(cameraId));
    }

    @Transactional(readOnly = true)
    public List<CameraEventDto> getEventHistory(Integer cameraId) {
        findEntityById(cameraId);
        return eventMapper.toDtoList(
            cameraEventRepository.findByCameraIdOrderByReceivedAtDesc(cameraId));
    }

    @Transactional(readOnly = true)
    public List<CameraDto> getOfflineCameras() {
        return cameraMapper.toDtoList(cameraRepository.findByIsOnlineFalseAndIsActiveTrue());
    }

    // ── job-level operations ──────────────────────────────────────────────

    /**
     * Marks cameras offline when no heartbeat received within OFFLINE_THRESHOLD_MIN.
     * Called by CameraHeartbeatJob every 5 minutes.
     */
    @Transactional
    public int markStaleAsOffline() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(OFFLINE_THRESHOLD_MIN);
        return cameraRepository.markStaleAsOffline(threshold);
    }

    /**
     * Retries failed/unprocessed camera events (up to MAX_RETRIES attempts).
     * Called by CameraHeartbeatJob every 15 minutes.
     */
    @Transactional
    public int retryFailedEvents() {
        List<CameraEventEntity> pending =
            cameraEventRepository.findUnprocessedForRetry(MAX_RETRIES);

        int processed = 0;
        for (CameraEventEntity event : pending) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om =
                    new com.fasterxml.jackson.databind.ObjectMapper();
                MqttEventPayload payload = om.readValue(event.getPayload(), MqttEventPayload.class);
                processorService.process(event, event.getCamera(), payload);
                processed++;
            } catch (Exception e) {
                cameraEventRepository.markFailed(event.getId(), "Retry failed: " + e.getMessage());
            }
        }
        return processed;
    }
}
