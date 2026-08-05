package com.academy.trafficviolationsystem.camera;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Entry point for all MQTT messages from traffic cameras and radars.
 *
 * Spring Integration routes every message arriving on "mqttInputChannel"
 * (configured in MqttConfig) to the handleMessage() method via
 * @ServiceActivator. This runs on the MQTT client thread.
 *
 * Responsibility of this class is intentionally narrow:
 *   1. Parse the raw JSON payload into MqttEventPayload.
 *   2. Route to the correct handler based on topic suffix:
 *        cameras/.../events    → handleDetectionEvent()
 *        cameras/.../heartbeat → handleHeartbeat()
 *   3. Persist a raw CameraEventEntity immediately (write-first).
 *   4. Hand off to CameraEventProcessorService for violation creation.
 *
 * The write-first pattern means even if the processor crashes, the raw
 * event is already in the database and can be retried by a job later.
 * This class never throws — all exceptions are caught and logged so the
 * MQTT client thread is never interrupted.
 */
@Component
public class MqttViolationListener {

    private static final Logger log = LoggerFactory.getLogger(MqttViolationListener.class);
    private static final String EVENTS_SUFFIX    = "/events";
    private static final String HEARTBEAT_SUFFIX = "/heartbeat";

    private final CameraRepository              cameraRepository;
    private final CameraEventRepository         cameraEventRepository;
    private final CameraEventProcessorService   processorService;
    private final ObjectMapper                  objectMapper;

    public MqttViolationListener(CameraRepository cameraRepository,
                                  CameraEventRepository cameraEventRepository,
                                  CameraEventProcessorService processorService,
                                  ObjectMapper objectMapper) {
        this.cameraRepository      = cameraRepository;
        this.cameraEventRepository = cameraEventRepository;
        this.processorService      = processorService;
        this.objectMapper          = objectMapper;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    @Transactional
    public void handleMessage(Message<String> message) {
        String topic   = (String) message.getHeaders().get("mqtt_receivedTopic");
        String rawJson = message.getPayload();

        if (topic == null) {
            log.warn("Received MQTT message with no topic header — skipping");
            return;
        }

        log.debug("MQTT message received on topic: {}", topic);

        try {
            if (topic.endsWith(HEARTBEAT_SUFFIX)) {
                handleHeartbeat(topic, rawJson);
            } else if (topic.endsWith(EVENTS_SUFFIX)) {
                handleDetectionEvent(topic, rawJson);
            } else {
                log.debug("Ignoring MQTT message on unrecognised topic pattern: {}", topic);
            }
        } catch (Exception e) {
            // Never let an exception propagate to the MQTT client thread
            log.error("Unhandled error processing MQTT message on topic {}: {}", topic, e.getMessage(), e);
        }
    }

    // ── detection event ───────────────────────────────────────────────────

    private void handleDetectionEvent(String topic, String rawJson) {
        // Look up the camera by MQTT topic
        CameraEntity camera = cameraRepository.findByMqttTopic(topic).orElse(null);
        if (camera == null) {
            log.warn("No camera registered for MQTT topic: {} — event ignored", topic);
            return;
        }
        if (!camera.isActive()) {
            log.debug("Camera {} is decommissioned — event ignored", camera.getSerialNumber());
            return;
        }

        // Parse the payload
        MqttEventPayload payload;
        try {
            payload = objectMapper.readValue(rawJson, MqttEventPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse MQTT payload from camera {}: {}", camera.getSerialNumber(), e.getMessage());
            persistFailedEvent(camera, topic, rawJson, "JSON parse error: " + e.getMessage());
            return;
        }

        // Persist raw event immediately (write-first)
        CameraEventEntity event = persistEvent(camera, topic, rawJson, payload);

        // Hand off to processor — it creates the ViolationEntity
        // Processor runs in its own transaction so a failure here doesn't
        // roll back the already-persisted raw event row.
        try {
            processorService.process(event, camera, payload);
        } catch (Exception e) {
            log.error("Processor failed for event {} from camera {}: {}",
                event.getId(), camera.getSerialNumber(), e.getMessage());
            cameraEventRepository.markFailed(event.getId(), e.getMessage());
        }
    }

    // ── heartbeat ─────────────────────────────────────────────────────────

    private void handleHeartbeat(String topic, String rawJson) {
        // Derive the events topic from the heartbeat topic to look up the camera
        String eventsTopic = topic.replace(HEARTBEAT_SUFFIX, EVENTS_SUFFIX);
        CameraEntity camera = cameraRepository.findByMqttTopic(eventsTopic).orElse(null);

        if (camera == null) {
            log.debug("No camera found for heartbeat topic: {} — ignoring", topic);
            return;
        }

        try {
            MqttEventPayload hb = objectMapper.readValue(rawJson, MqttEventPayload.class);
            if (hb.getFirmwareVersion() != null) {
                cameraRepository.recordHeartbeatWithFirmware(
                        Math.toIntExact(camera.getId()), LocalDateTime.now(), hb.getFirmwareVersion());
            } else {
                cameraRepository.recordHeartbeat(Math.toIntExact(camera.getId()), LocalDateTime.now());
            }
            log.debug("Heartbeat recorded for camera {}", camera.getSerialNumber());
        } catch (Exception e) {
            // Heartbeat failure is non-critical — just record the timestamp
            cameraRepository.recordHeartbeat(Math.toIntExact(camera.getId()), LocalDateTime.now());
            log.debug("Heartbeat parsed with error for {}: {}", camera.getSerialNumber(), e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private CameraEventEntity persistEvent(CameraEntity camera, String topic,
                                            String rawJson, MqttEventPayload payload) {
        CameraEventEntity event = new CameraEventEntity();
        event.setCamera(camera);
        event.setMqttTopic(topic);
        event.setPayload(rawJson);
        event.setReceivedAt(LocalDateTime.now());
        event.setLicensePlate(payload.getPlate());
        event.setMeasuredSpeed(payload.getMeasuredSpeedKmh());
        event.setEventLatitude(payload.getLatitude());
        event.setEventLongitude(payload.getLongitude());
        event.setImageUrl(payload.getImageUrl());
        event.setProcessed(false);
        return cameraEventRepository.save(event);
    }

    private void persistFailedEvent(CameraEntity camera, String topic,
                                     String rawJson, String error) {
        CameraEventEntity event = new CameraEventEntity();
        event.setCamera(camera);
        event.setMqttTopic(topic);
        event.setPayload(rawJson);
        event.setReceivedAt(LocalDateTime.now());
        event.setProcessed(false);
        event.setProcessingError(error);
        cameraEventRepository.save(event);
    }
}
