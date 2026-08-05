package com.academy.trafficviolationsystem.rodezone;

import com.academy.trafficviolationsystem.core.entities.AutoIdBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(
    name = "road_zones",
    indexes = {
        @Index(name = "idx_zone_type", columnList = "zone_type, is_active")
    },
    uniqueConstraints = {}
)
@Check(
        name = "chk_zone_shape",
        constraints = "(radius_meters IS NOT NULL AND geo_json_boundary IS NULL AND center_latitude IS NOT NULL AND center_longitude IS NOT NULL) " +
                "OR (radius_meters IS NULL AND geo_json_boundary IS NOT NULL)"
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RoadZoneEntity extends AutoIdBaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false)
    private ZoneType zoneType;

    @Column(nullable = false)
    private int speedLimitKmh;

    @Column(nullable = true)
    private String description;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(name = "center_latitude", nullable = true)
    private Double centerLatitude;

    @Column(name = "center_longitude", nullable = true)
    private Double centerLongitude;

    /** Radius in metres — used for simple circular zones. */
    @Column(nullable = true)
    private Integer radiusMeters;

    /** Full GeoJSON polygon string for complex zone shapes. */
    @Column(name = "geo_json_boundary", columnDefinition = "TEXT", nullable = true)
    private String geoJsonBoundary;
}
