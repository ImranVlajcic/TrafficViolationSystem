package com.academy.trafficviolationsystem.unit;

import com.academy.trafficviolationsystem.camera.*;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.camera.ConflictException;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CameraService.
 *
 * These target the logic CameraService actually owns: the beforeInsert /
 * beforeUpdate hooks, additionalFilter, the maintenance-log operations,
 * the read helpers, and the job-level operations (markStaleAsOffline,
 * retryFailedEvents, decommission).
 * insert()/update() themselves are default methods on BaseCRUDService and
 * are exercised indirectly here (through beforeInsert/beforeUpdate), not
 * re-tested directly.
 *
 * All collaborators are mocked — no Spring context, no database.
 *
 * NOTE: findEntityById() is inherited from BaseCRUDService and is assumed
 * here to resolve via cameraRepository.findById(id), matching the pattern
 * used in UserServiceTest. Share BaseCRUDService.java if it works
 * differently and these tests can be adjusted.
 */
@ExtendWith(MockitoExtension.class)
class CameraServiceTest {

    @Mock private CameraRepository cameraRepository;
    @Mock private CameraEventRepository cameraEventRepository;
    @Mock private CameraMaintenanceLogRepository maintenanceRepository;
    @Mock private UserRepository userRepository;
    @Mock private CameraMapper cameraMapper;
    @Mock private CameraEventMapper eventMapper;
    @Mock private CameraMaintenanceLogMapper maintenanceMapper;
    @Mock private CameraEventProcessorService processorService;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private CameraService cameraService;

    private CameraEntity speedRadar;
    private CameraEntity redLightCamera;

    @BeforeEach
    void setUp() {
        speedRadar = new CameraEntity();
        speedRadar.setId(1L);
        speedRadar.setSerialNumber("ILZ-RADAR-001");
        speedRadar.setName("Ilidza Radar");
        speedRadar.setCameraType(CameraType.SPEED_RADAR);
        speedRadar.setLatitude(43.8563);
        speedRadar.setLongitude(18.4131);
        speedRadar.setMqttTopic("cameras/ILZ-RADAR-001/events");
        speedRadar.setFirmwareVersion("1.0.0");
        speedRadar.setActive(true);

        redLightCamera = new CameraEntity();
        redLightCamera.setId(2L);
        redLightCamera.setSerialNumber("BJL-RL-014");
        redLightCamera.setName("Baščaršija Red Light");
        redLightCamera.setCameraType(CameraType.RED_LIGHT);
        redLightCamera.setLatitude(43.8590);
        redLightCamera.setLongitude(18.4318);
        redLightCamera.setMqttTopic("cameras/BJL-RL-014/events");
        redLightCamera.setFirmwareVersion("2.4.1");
        redLightCamera.setActive(true);
    }

    // ───────────────────────────── beforeInsert ─────────────────────────────

    @Nested
    class BeforeInsertTests {

        @Test
        void throwsWhenSerialNumberAlreadyRegistered() {
            CameraCreateRequest req = new CameraCreateRequest();
            req.setSerialNumber("ILZ-RADAR-001");
            req.setMqttTopic("cameras/ILZ-RADAR-001/events");

            when(cameraRepository.existsBySerialNumber("ILZ-RADAR-001")).thenReturn(true);

            assertThatThrownBy(() -> cameraService.beforeInsert(req, new CameraEntity()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already registered");

            verify(cameraRepository, never()).existsByMqttTopic(anyString());
        }

        @Test
        void throwsWhenMqttTopicAlreadyInUse() {
            CameraCreateRequest req = new CameraCreateRequest();
            req.setSerialNumber("NEW-CAM-001");
            req.setMqttTopic("cameras/NEW-CAM-001/events");

            when(cameraRepository.existsBySerialNumber("NEW-CAM-001")).thenReturn(false);
            when(cameraRepository.existsByMqttTopic("cameras/NEW-CAM-001/events")).thenReturn(true);

            assertThatThrownBy(() -> cameraService.beforeInsert(req, new CameraEntity()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already in use");
        }

        @Test
        void throwsWhenMqttTopicDoesNotFollowConvention() {
            CameraCreateRequest req = new CameraCreateRequest();
            req.setSerialNumber("NEW-CAM-001");
            req.setMqttTopic("wrong/topic/path");

            when(cameraRepository.existsBySerialNumber("NEW-CAM-001")).thenReturn(false);
            when(cameraRepository.existsByMqttTopic("wrong/topic/path")).thenReturn(false);

            assertThatThrownBy(() -> cameraService.beforeInsert(req, new CameraEntity()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("MQTT topic must follow the convention");
        }

        @Test
        void allowsValidRequestFollowingTopicConvention() {
            CameraCreateRequest req = new CameraCreateRequest();
            req.setSerialNumber("NEW-CAM-001");
            req.setMqttTopic("cameras/NEW-CAM-001/events");

            when(cameraRepository.existsBySerialNumber("NEW-CAM-001")).thenReturn(false);
            when(cameraRepository.existsByMqttTopic("cameras/NEW-CAM-001/events")).thenReturn(false);

            cameraService.beforeInsert(req, new CameraEntity()); // should not throw
        }
    }

    // ───────────────────────────── beforeUpdate ─────────────────────────────

    @Nested
    class BeforeUpdateTests {

        @Test
        void isANoOpAndNeverTouchesCollaborators() {
            CameraUpdateRequest req = new CameraUpdateRequest();
            req.setName("Renamed Camera");

            cameraService.beforeUpdate(req, speedRadar); // should not throw

            verifyNoInteractions(cameraRepository, cameraEventRepository,
                    maintenanceRepository, userRepository);
        }
    }

    // ───────────────────────────── additionalFilter ─────────────────────────────

    @Nested
    class AdditionalFilterTests {

        @Test
        @SuppressWarnings("unchecked")
        void buildsFreeTextSearchPredicateAcrossThreeFields() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<CameraEntity> root = mock(Root.class);
            Path<Object> path = mock(Path.class);
            Expression<String> lowered = mock(Expression.class);
            Predicate likePredicate = mock(Predicate.class);
            Predicate orPredicate = mock(Predicate.class);

            when(root.<Object>get(anyString())).thenReturn(path);
            when(cb.lower(any())).thenReturn(lowered);
            when(cb.like(any(), anyString())).thenReturn(likePredicate);
            when(cb.or(any(Predicate.class), any(Predicate.class), any(Predicate.class)))
                    .thenReturn(orPredicate);

            CameraSearchObject searchObj = new CameraSearchObject();
            searchObj.setSearch("radar");

            List<Predicate> predicates = cameraService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactly(orPredicate);
            // name, serialNumber, locationDescription → 3 LIKE clauses, same pattern
            verify(cb, times(3)).like(any(), eq("%radar%"));
        }

        @Test
        void ignoresBlankSearchTerm() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<CameraEntity> root = mock(Root.class);
            CameraSearchObject searchObj = new CameraSearchObject();
            searchObj.setSearch("   ");

            List<Predicate> predicates = cameraService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).isEmpty();
            verifyNoInteractions(root);
        }

        @Test
        @SuppressWarnings("unchecked")
        void addsCameraTypeIsOnlineAndIsActiveFiltersWhenPresent() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<CameraEntity> root = mock(Root.class);
            Path<Object> typePath = mock(Path.class);
            Path<Object> onlinePath = mock(Path.class);
            Path<Object> activePath = mock(Path.class);
            Predicate typePredicate = mock(Predicate.class);
            Predicate onlinePredicate = mock(Predicate.class);
            Predicate activePredicate = mock(Predicate.class);

            when(root.<Object>get("cameraType")).thenReturn(typePath);
            when(root.<Object>get("isOnline")).thenReturn(onlinePath);
            when(root.<Object>get("isActive")).thenReturn(activePath);
            when(cb.equal(typePath, CameraType.SPEED_RADAR)).thenReturn(typePredicate);
            when(cb.equal(onlinePath, false)).thenReturn(onlinePredicate);
            when(cb.equal(activePath, true)).thenReturn(activePredicate);

            CameraSearchObject searchObj = new CameraSearchObject();
            searchObj.setCameraType(CameraType.SPEED_RADAR);
            searchObj.setIsOnline(false);
            searchObj.setIsActive(true);

            List<Predicate> predicates = cameraService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).containsExactlyInAnyOrder(typePredicate, onlinePredicate, activePredicate);
        }

        @Test
        void returnsEmptyListWhenNoFiltersProvided() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Root<CameraEntity> root = mock(Root.class);
            CameraSearchObject searchObj = new CameraSearchObject();

            List<Predicate> predicates = cameraService.additionalFilter(cb, searchObj, root);

            assertThat(predicates).isEmpty();
        }
    }

    // ───────────────────────────── logMaintenance ─────────────────────────────

    @Nested
    class LogMaintenanceTests {

        private UserPrincipal principal;
        private UUID technicianId;
        private UserEntity technician;

        @BeforeEach
        void setUpPrincipal() {
            technicianId = UUID.randomUUID();

            technician = new UserEntity();
            technician.setId(technicianId);

            principal = mock(UserPrincipal.class);
        }

        @Test
        void updatesCameraFirmwareWhenFirmwareAfterProvided() {
            when(principal.getId()).thenReturn(technicianId);

            LogMaintenanceRequest req = new LogMaintenanceRequest();
            req.setMaintenanceType(MaintenanceType.FIRMWARE_UPDATE);
            req.setFirmwareAfter("2.0.0");

            when(cameraRepository.findById(1))
                    .thenReturn(Optional.of(speedRadar));

            when(userRepository.findById(technicianId))
                    .thenReturn(Optional.of(technician));

            when(maintenanceRepository.save(any(CameraMaintenanceLogEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            cameraService.logMaintenance(1, req, principal);

            assertThat(speedRadar.getFirmwareVersion())
                    .isEqualTo("2.0.0");

            verify(cameraRepository).save(speedRadar);
        }

        @Test
        void doesNotTouchCameraFirmwareWhenFirmwareAfterIsBlank() {
            when(principal.getId()).thenReturn(technicianId);

            LogMaintenanceRequest req = new LogMaintenanceRequest();
            req.setMaintenanceType(MaintenanceType.PHYSICAL_INSPECTION);
            req.setFirmwareAfter("   ");

            when(cameraRepository.findById(1))
                    .thenReturn(Optional.of(speedRadar));

            when(userRepository.findById(technicianId))
                    .thenReturn(Optional.of(technician));

            when(maintenanceRepository.save(any(CameraMaintenanceLogEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            cameraService.logMaintenance(1, req, principal);

            assertThat(speedRadar.getFirmwareVersion())
                    .isEqualTo("1.0.0");

            verify(cameraRepository, never()).save(any());
        }

        @Test
        void fallsBackToCameraFirmwareWhenFirmwareBeforeNotProvided() {
            when(principal.getId()).thenReturn(technicianId);

            LogMaintenanceRequest req = new LogMaintenanceRequest();
            req.setMaintenanceType(MaintenanceType.CALIBRATION);
            // firmwareBefore deliberately left unset

            when(cameraRepository.findById(1))
                    .thenReturn(Optional.of(speedRadar));

            when(userRepository.findById(technicianId))
                    .thenReturn(Optional.of(technician));

            when(maintenanceRepository.save(any(CameraMaintenanceLogEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<CameraMaintenanceLogEntity> captor =
                    ArgumentCaptor.forClass(CameraMaintenanceLogEntity.class);

            cameraService.logMaintenance(1, req, principal);

            verify(maintenanceRepository).save(captor.capture());

            assertThat(captor.getValue().getFirmwareBefore())
                    .isEqualTo("1.0.0");
        }

        @Test
        void usesExplicitFirmwareBeforeWhenProvided() {
            when(principal.getId()).thenReturn(technicianId);

            LogMaintenanceRequest req = new LogMaintenanceRequest();
            req.setMaintenanceType(MaintenanceType.CALIBRATION);
            req.setFirmwareBefore("0.9.0");

            when(cameraRepository.findById(1))
                    .thenReturn(Optional.of(speedRadar));

            when(userRepository.findById(technicianId))
                    .thenReturn(Optional.of(technician));

            when(maintenanceRepository.save(any(CameraMaintenanceLogEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<CameraMaintenanceLogEntity> captor =
                    ArgumentCaptor.forClass(CameraMaintenanceLogEntity.class);

            cameraService.logMaintenance(1, req, principal);

            verify(maintenanceRepository).save(captor.capture());

            assertThat(captor.getValue().getFirmwareBefore())
                    .isEqualTo("0.9.0");
        }

        @Test
        void setsCompletedAtWhenCompletedIsTrue() {
            when(principal.getId()).thenReturn(technicianId);

            LogMaintenanceRequest req = new LogMaintenanceRequest();
            req.setMaintenanceType(MaintenanceType.FAULT_REPAIR);
            req.setCompleted(true);

            when(cameraRepository.findById(1))
                    .thenReturn(Optional.of(speedRadar));

            when(userRepository.findById(technicianId))
                    .thenReturn(Optional.of(technician));

            when(maintenanceRepository.save(any(CameraMaintenanceLogEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<CameraMaintenanceLogEntity> captor =
                    ArgumentCaptor.forClass(CameraMaintenanceLogEntity.class);

            cameraService.logMaintenance(1, req, principal);

            verify(maintenanceRepository).save(captor.capture());

            assertThat(captor.getValue().getCompletedAt())
                    .isNotNull();
        }

        @Test
        void doesNotSetCompletedAtWhenScheduledForLater() {
            when(principal.getId()).thenReturn(technicianId);

            LogMaintenanceRequest req = new LogMaintenanceRequest();
            req.setMaintenanceType(MaintenanceType.HARDWARE_REPLACEMENT);
            req.setCompleted(false);

            when(cameraRepository.findById(1))
                    .thenReturn(Optional.of(speedRadar));

            when(userRepository.findById(technicianId))
                    .thenReturn(Optional.of(technician));

            when(maintenanceRepository.save(any(CameraMaintenanceLogEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<CameraMaintenanceLogEntity> captor =
                    ArgumentCaptor.forClass(CameraMaintenanceLogEntity.class);

            cameraService.logMaintenance(1, req, principal);

            verify(maintenanceRepository).save(captor.capture());

            assertThat(captor.getValue().getCompletedAt())
                    .isNull();
        }

        @Test
        void allowsNullTechnicianWhenPrincipalUserNotFound() {
            when(principal.getId()).thenReturn(technicianId);

            LogMaintenanceRequest req = new LogMaintenanceRequest();
            req.setMaintenanceType(MaintenanceType.PHYSICAL_INSPECTION);

            when(cameraRepository.findById(1))
                    .thenReturn(Optional.of(speedRadar));

            when(userRepository.findById(technicianId))
                    .thenReturn(Optional.empty());

            when(maintenanceRepository.save(any(CameraMaintenanceLogEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<CameraMaintenanceLogEntity> captor =
                    ArgumentCaptor.forClass(CameraMaintenanceLogEntity.class);

            cameraService.logMaintenance(1, req, principal);

            verify(maintenanceRepository).save(captor.capture());

            assertThat(captor.getValue().getPerformedBy())
                    .isNull();
        }

        @Test
        void throwsWhenCameraNotFound() {
            LogMaintenanceRequest req = new LogMaintenanceRequest();
            req.setMaintenanceType(MaintenanceType.PHYSICAL_INSPECTION);

            when(cameraRepository.findById(99))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    cameraService.logMaintenance(99, req, principal)
            )
                    .isInstanceOf(NotFoundException.class);

            verifyNoInteractions(maintenanceRepository);
        }
    }

    // ───────────────────────────── completeMaintenance ─────────────────────────────

    @Nested
    class CompleteMaintenanceTests {

        private UserPrincipal principal;
        private UUID technicianId;
        private UserEntity technician;
        private CameraMaintenanceLogEntity log;

        @BeforeEach
        void setUpLog() {
            technicianId = UUID.randomUUID();
            technician = new UserEntity();
            technician.setId(technicianId);

            principal = mock(UserPrincipal.class);

            log = new CameraMaintenanceLogEntity();
            log.setId(UUID.randomUUID());
            log.setCamera(speedRadar);
            log.setMaintenanceType(MaintenanceType.CALIBRATION);
            log.setCompleted(false);
        }

        @Test
        void throwsWhenLogNotFound() {
            UUID logId = UUID.randomUUID();
            when(maintenanceRepository.findById(logId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cameraService.completeMaintenance(1L, logId, principal))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsWhenLogBelongsToDifferentCamera() {
            when(maintenanceRepository.findById(log.getId())).thenReturn(Optional.of(log));

            assertThatThrownBy(() -> cameraService.completeMaintenance(999L, log.getId(), principal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        void throwsWhenAlreadyCompleted() {
            log.setCompleted(true);
            when(maintenanceRepository.findById(log.getId())).thenReturn(Optional.of(log));

            assertThatThrownBy(() -> cameraService.completeMaintenance(1L, log.getId(), principal))
                    .isInstanceOf(ConflictException.class);

            verify(maintenanceRepository, never()).save(any());
        }

        @Test
        void completesAndSetsPerformedByWhenNotAlreadySet() {
            when(maintenanceRepository.findById(log.getId())).thenReturn(Optional.of(log));
            when(principal.getId()).thenReturn(technicianId);
            when(userRepository.findById(technicianId)).thenReturn(Optional.of(technician));
            when(maintenanceRepository.save(any(CameraMaintenanceLogEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            cameraService.completeMaintenance(1L, log.getId(), principal);

            assertThat(log.isCompleted()).isTrue();
            assertThat(log.getCompletedAt()).isNotNull();
            assertThat(log.getPerformedBy()).isEqualTo(technician);
        }

        @Test
        void doesNotOverwritePerformedByWhenAlreadySet() {
            UserEntity originalTechnician = new UserEntity();
            originalTechnician.setId(UUID.randomUUID());

            speedRadar.setId(1L);
            log.setCamera(speedRadar);
            log.setPerformedBy(originalTechnician);

            assertThat(log.getCamera()).isNotNull();
            assertThat(log.getCamera().getId()).isEqualTo(1L);

            when(maintenanceRepository.findById(log.getId()))
                    .thenReturn(Optional.of(log));

            when(maintenanceRepository.save(any(CameraMaintenanceLogEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            cameraService.completeMaintenance(1L, log.getId(), principal);

            assertThat(log.getPerformedBy())
                    .isEqualTo(originalTechnician);

            verify(userRepository, never()).findById(any());
            verify(maintenanceRepository).save(log);
        }
    }

    // ───────────────────────────── read helpers ─────────────────────────────

    @Nested
    class ReadHelperTests {

        @Test
        void getMaintenanceHistoryThrowsWhenCameraNotFound() {
            when(cameraRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cameraService.getMaintenanceHistory(99))
                    .isInstanceOf(NotFoundException.class);

            verifyNoInteractions(maintenanceMapper);
        }

        @Test
        void getMaintenanceHistoryReturnsMappedListForExistingCamera() {
            List<CameraMaintenanceLogEntity> entities = List.of(new CameraMaintenanceLogEntity());
            List<CameraMaintenanceLogDto> dtos = List.of(new CameraMaintenanceLogDto());

            when(cameraRepository.findById(1)).thenReturn(Optional.of(speedRadar));
            when(maintenanceRepository.findByCameraIdOrderByCompletedAtDesc(1)).thenReturn(entities);
            when(maintenanceMapper.toDtoList(entities)).thenReturn(dtos);

            List<CameraMaintenanceLogDto> result = cameraService.getMaintenanceHistory(1);

            assertThat(result).isEqualTo(dtos);
        }

        @Test
        void getEventHistoryThrowsWhenCameraNotFound() {
            when(cameraRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cameraService.getEventHistory(99))
                    .isInstanceOf(NotFoundException.class);

            verifyNoInteractions(eventMapper);
        }

        @Test
        void getEventHistoryReturnsMappedListForExistingCamera() {
            List<CameraEventEntity> entities = List.of(new CameraEventEntity());
            List<CameraEventDto> dtos = List.of(new CameraEventDto());

            when(cameraRepository.findById(1)).thenReturn(Optional.of(speedRadar));
            when(cameraEventRepository.findByCameraIdOrderByReceivedAtDesc(1)).thenReturn(entities);
            when(eventMapper.toDtoList(entities)).thenReturn(dtos);

            List<CameraEventDto> result = cameraService.getEventHistory(1);

            assertThat(result).isEqualTo(dtos);
        }

        @Test
        void getOfflineCamerasReturnsMappedList() {
            List<CameraEntity> entities = List.of(speedRadar);
            List<CameraDto> dtos = List.of(new CameraDto());

            when(cameraRepository.findByIsOnlineFalseAndIsActiveTrue()).thenReturn(entities);
            when(cameraMapper.toDtoList(entities)).thenReturn(dtos);

            List<CameraDto> result = cameraService.getOfflineCameras();

            assertThat(result).isEqualTo(dtos);
        }
    }

    // ───────────────────────────── markStaleAsOffline ─────────────────────────────

    @Nested
    class MarkStaleAsOfflineTests {

        @Test
        void delegatesToRepositoryWithTenMinuteThreshold() {
            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            when(cameraRepository.markStaleAsOffline(any())).thenReturn(3);

            LocalDateTime before = LocalDateTime.now().minusMinutes(10);
            int result = cameraService.markStaleAsOffline();
            LocalDateTime after = LocalDateTime.now().minusMinutes(10);

            verify(cameraRepository).markStaleAsOffline(captor.capture());
            assertThat(captor.getValue()).isBetween(before, after);
            assertThat(result).isEqualTo(3);
        }
    }

    // ───────────────────────────── retryFailedEvents ─────────────────────────────

    @Nested
    class RetryFailedEventsTests {

        @Test
        void processesEachUnprocessedEventAndReturnsCount() {
            CameraEventEntity event1 = new CameraEventEntity();
            event1.setId(UUID.randomUUID());
            event1.setCamera(speedRadar);
            event1.setPayload("{\"plate\":\"A123BC\",\"measuredSpeedKmh\":90}");

            CameraEventEntity event2 = new CameraEventEntity();
            event2.setId(UUID.randomUUID());
            event2.setCamera(redLightCamera);
            event2.setPayload("{\"plate\":\"B456DE\"}");

            when(cameraEventRepository.findUnprocessedForRetry(3))
                    .thenReturn(List.of(event1, event2));

            int processed = cameraService.retryFailedEvents();

            assertThat(processed).isEqualTo(2);
            verify(processorService).process(eq(event1), eq(speedRadar), any(MqttEventPayload.class));
            verify(processorService).process(eq(event2), eq(redLightCamera), any(MqttEventPayload.class));
            verify(cameraEventRepository, never()).markFailed(any(), anyString());
        }

        @Test
        void marksEventFailedWhenPayloadIsMalformedJson() {
            CameraEventEntity badEvent = new CameraEventEntity();
            badEvent.setId(UUID.randomUUID());
            badEvent.setCamera(speedRadar);
            badEvent.setPayload("not-valid-json");

            when(cameraEventRepository.findUnprocessedForRetry(3))
                    .thenReturn(List.of(badEvent));

            int processed = cameraService.retryFailedEvents();

            assertThat(processed).isEqualTo(0);
            verify(cameraEventRepository).markFailed(eq(badEvent.getId()), anyString());
            verifyNoInteractions(processorService);
        }

        @Test
        void marksEventFailedWhenProcessorThrows() {
            CameraEventEntity event = new CameraEventEntity();
            event.setId(UUID.randomUUID());
            event.setCamera(speedRadar);
            event.setPayload("{\"plate\":\"A123BC\"}");

            when(cameraEventRepository.findUnprocessedForRetry(3))
                    .thenReturn(List.of(event));
            doThrow(new RuntimeException("boom"))
                    .when(processorService).process(eq(event), eq(speedRadar), any(MqttEventPayload.class));

            int processed = cameraService.retryFailedEvents();

            assertThat(processed).isEqualTo(0);
            verify(cameraEventRepository).markFailed(eq(event.getId()), contains("boom"));
        }

        @Test
        void returnsZeroWhenNothingToRetry() {
            when(cameraEventRepository.findUnprocessedForRetry(3)).thenReturn(List.of());

            int processed = cameraService.retryFailedEvents();

            assertThat(processed).isZero();
            verifyNoInteractions(processorService);
        }
    }

    // ───────────────────────────── decommission ─────────────────────────────

    @Nested
    class DecommissionTests {

        @Test
        void deletesCameraWhenFound() {
            when(cameraRepository.findById(1)).thenReturn(Optional.of(speedRadar));

            cameraService.decommission(1);

            verify(cameraRepository).delete(speedRadar);
        }

        @Test
        void throwsWhenCameraNotFound() {
            when(cameraRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cameraService.decommission(99))
                    .isInstanceOf(NotFoundException.class);

            verify(cameraRepository, never()).delete(any());
        }
    }
}