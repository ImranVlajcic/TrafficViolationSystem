package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.camera.CameraEntity;
import com.academy.trafficviolationsystem.camera.CameraEventEntity;
import com.academy.trafficviolationsystem.camera.CameraEventRepository;
import com.academy.trafficviolationsystem.vehicle.VehicleEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds raw camera events — the append-only buffer MqttViolationListener
 * would normally write. ~45 events: most get consumed into a ViolationEntity
 * by ViolationSeeder (processed=true), a handful are left unprocessed or
 * failed to exercise the retry-job frontend/admin views.
 */
@Component
public class CameraEventSeeder {

    private static final int EVENT_COUNT = 45;

    private final CameraEventRepository cameraEventRepository;

    public CameraEventSeeder(CameraEventRepository cameraEventRepository) {
        this.cameraEventRepository = cameraEventRepository;
    }

    public List<CameraEventEntity> seed(List<CameraEntity> cameras, List<VehicleEntity> vehicles) {
        if (cameraEventRepository.count() > 0) {
            return cameraEventRepository.findAll();
        }

        List<CameraEventEntity> events = new ArrayList<>();

        for (int i = 0; i < EVENT_COUNT; i++) {
            CameraEntity camera = SeedRandom.pick(cameras);
            VehicleEntity vehicle = SeedRandom.pick(vehicles);
            LocalDateTime receivedAt = SeedRandom.pastDateTime(1, 180);

            boolean ocrFailed = SeedRandom.chance(0.05);
            boolean hasGps = SeedRandom.chance(0.6);

            CameraEventEntity event = new CameraEventEntity();
            event.setMqttTopic(camera.getMqttTopic());
            event.setPayload(buildFakePayload(camera, vehicle, receivedAt));
            event.setReceivedAt(receivedAt);
            event.setLicensePlate(ocrFailed ? null : vehicle.getLicensePlate());
            event.setMeasuredSpeed(camera.getSpeedLimitKmh() == null
                    ? null
                    : camera.getSpeedLimitKmh() + SeedRandom.intBetween(-5, 60));
            event.setEventLatitude(hasGps ? camera.getLatitude() : null);
            event.setEventLongitude(hasGps ? camera.getLongitude() : null);
            event.setImageUrl("https://cdn.traffic-academy.local/events/evt-" + (i + 1) + ".jpg");
            event.setProcessed(false);
            event.setRetryCount(0);
            event.setCamera(camera);

            events.add(cameraEventRepository.save(event));
        }

        return events;
    }

    private String buildFakePayload(CameraEntity camera, VehicleEntity vehicle, LocalDateTime receivedAt) {
        return "{\"camera\":\"" + camera.getSerialNumber() + "\","
                + "\"plate\":\"" + vehicle.getLicensePlate() + "\","
                + "\"timestamp\":\"" + receivedAt + "\"}";
    }
}