package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.rodezone.RoadZoneEntity;
import com.academy.trafficviolationsystem.rodezone.RoadZoneRepository;
import com.academy.trafficviolationsystem.rodezone.ZoneType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds a spread of road zones around Sarajevo/Mostar-area coordinates.
 * Note: the package name "rodezone" and the ZoneType value "RESIEDNTAL"
 * are spelled exactly as they appear in the real entity/enum — not typos
 * introduced here.
 */
@Component
public class RoadZoneSeeder {

    private final RoadZoneRepository roadZoneRepository;

    public RoadZoneSeeder(RoadZoneRepository roadZoneRepository) {
        this.roadZoneRepository = roadZoneRepository;
    }

    /** Zones created here, returned so CameraSeeder can attach cameras to them. */
    public List<RoadZoneEntity> seed() {
        if (roadZoneRepository.count() > 0) {
            return roadZoneRepository.findAll();
        }

        List<RoadZoneEntity> zones = new ArrayList<>();

        zones.add(zone("OŠ Skenderija — školska zona", ZoneType.SCHOOL, 30,
                43.8586, 18.4131, 150, "Škole u blizini centra Sarajeva"));
        zones.add(zone("Grbavica — stambena zona", ZoneType.RESIDENTIAL, 40,
                43.8480, 18.3908, 300, "Gusto naseljeno stambeno naselje"));
        zones.add(zone("Autoput A1 — Sarajevo-Zenica", ZoneType.HIGHWAY, 100,
                43.9800, 18.2500, 2000, "Dionica autoputa prema Zenici"));
        zones.add(zone("Centar Sarajevo — gradsko jezgro", ZoneType.CITY_CENTER, 50,
                43.8600, 18.4300, 500, "Baščaršija i okolina"));
        zones.add(zone("KCUS — bolnička zona", ZoneType.HOSPITAL, 30,
                43.8460, 18.4210, 200, "Klinički centar Univerziteta u Sarajevu"));
        zones.add(zone("Rekonstrukcija mosta — Vrbanja", ZoneType.CONSTRUCTION, 30,
                43.8555, 18.4059, 100, "Privremeno gradilište, smanjeno ograničenje"));
        zones.add(zone("Industrijska zona Rajlovac", ZoneType.INDUSTRIAL, 50,
                43.8938, 18.3339, 800, "Teretni saobraćaj, skladišta"));
        zones.add(zone("OŠ Mostar Centar — školska zona", ZoneType.SCHOOL, 30,
                43.3438, 17.8078, 150, "Škole u centru Mostara"));
        zones.add(zone("Mostar — stambena zona", ZoneType.RESIDENTIAL, 40,
                43.3475, 17.8081, 350, "Naselje uz obalu Neretve"));
        zones.add(zone("Banja Luka — gradsko jezgro", ZoneType.CITY_CENTER, 50,
                44.7722, 17.1910, 500, "Centar grada Banja Luka"));
        zones.add(zone("Autoput Banja Luka — Doboj", ZoneType.HIGHWAY, 100,
                44.9000, 17.7000, 2000, "Magistralni pravac"));
        zones.add(zone("UKC Tuzla — bolnička zona", ZoneType.HOSPITAL, 30,
                44.5386, 18.6739, 200, "Univerzitetski klinički centar Tuzla"));

        for (RoadZoneEntity zone : zones) {
            roadZoneRepository.save(zone);
        }
        return zones;
    }

    private RoadZoneEntity zone(String name, ZoneType type, int speedLimit,
                                double lat, double lng, int radius, String description) {
        RoadZoneEntity zone = new RoadZoneEntity();
        zone.setName(name);
        zone.setZoneType(type);
        zone.setSpeedLimitKmh(speedLimit);
        zone.setDescription(description);
        zone.setActive(true);
        zone.setCenterLatitude(lat);
        zone.setCenterLongitude(lng);
        zone.setRadiusMeters(radius);
        zone.setGeoJsonBoundary(null);
        return zone;
    }
}