package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.camera.CameraEntity;
import com.academy.trafficviolationsystem.camera.CameraRepository;
import com.academy.trafficviolationsystem.camera.CameraType;
import com.academy.trafficviolationsystem.rodezone.RoadZoneEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds 18 cameras spread across the seeded road zones.
 *
 * NOTE: CameraEntity.zoneId is an Integer while RoadZoneEntity's real id
 * is a Long (AutoIdBaseEntity). This works for seed-scale IDs but is a
 * latent type mismatch — flagged separately, not fixed here.
 */
@Component
public class CameraSeeder {

    private final CameraRepository cameraRepository;

    public CameraSeeder(CameraRepository cameraRepository) {
        this.cameraRepository = cameraRepository;
    }

    public List<CameraEntity> seed(List<RoadZoneEntity> zones) {
        if (cameraRepository.count() > 0) {
            return cameraRepository.findAll();
        }

        List<CameraEntity> cameras = new ArrayList<>();
        CameraType[] types = {
                CameraType.SPEED_RADAR, CameraType.ANPR, CameraType.RED_LIGHT,
                CameraType.MOBILE_RADAR, CameraType.OVERHEAD
        };

        int serial = 1;
        for (int i = 0; i < 18; i++) {
            RoadZoneEntity zone = zones.get(i % zones.size());
            CameraType type = types[i % types.length];

            CameraEntity camera = new CameraEntity();
            camera.setSerialNumber(String.format("CAM-%04d", serial));
            camera.setName(zone.getName() + " — kamera " + (i + 1));
            camera.setCameraType(type);
            camera.setLatitude(zone.getCenterLatitude() + SeedRandom.RNG.nextDouble() * 0.004 - 0.002);
            camera.setLongitude(zone.getCenterLongitude() + SeedRandom.RNG.nextDouble() * 0.004 - 0.002);
            camera.setDirectionDegrees(type == CameraType.ANPR ? null : SeedRandom.intBetween(0, 359));
            camera.setLocationDescription(zone.getName());
            camera.setSpeedLimitKmh(
                    type == CameraType.RED_LIGHT || type == CameraType.ANPR ? null : zone.getSpeedLimitKmh()
            );
            camera.setMqttTopic("cameras/" + camera.getSerialNumber() + "/events");
            camera.setOnline(SeedRandom.chance(0.9));
            camera.setLastHeartbeatAt(camera.isOnline()
                    ? LocalDateTime.now().minusMinutes(SeedRandom.intBetween(0, 8))
                    : LocalDateTime.now().minusHours(SeedRandom.intBetween(2, 72)));
            camera.setActive(SeedRandom.chance(0.95));
            camera.setInstallDate(LocalDate.now().minusYears(SeedRandom.intBetween(1, 5)));
            camera.setFirmwareVersion("v" + SeedRandom.intBetween(1, 4) + "." + SeedRandom.intBetween(0, 9));
            camera.setZoneId(zone.getId().intValue());

            cameras.add(cameraRepository.save(camera));
            serial++;
        }
        return cameras;
    }
}