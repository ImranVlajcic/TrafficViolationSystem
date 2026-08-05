package com.academy.trafficviolationsystem.rodezone;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoadZoneDto {

    private Integer id;
    private String name;
    private ZoneType zoneType;
    private int speedLimitKmh;
    private String description;
    private boolean isActive;
    private Double centerLatitude;
    private Double centerLongitude;
    private Integer radiusMeters;
    private String geoJsonBoundary;

    /** Populated by RoadZoneService — count of cameras currently assigned to this zone. */
    private Integer cameraCount;
}
