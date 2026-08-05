package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.core.entities.AutoIdBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "accident_hotspots",
    indexes = {
        @Index(name = "idx_hotspot_location", columnList = "latitude, longitude")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccidentHotspotEntity extends AutoIdBaseEntity {

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private int radiusMeters = 100;

    @Column(nullable = false)
    private int violationCount;

    @Column(nullable = true)
    private String dominantType;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private Double severityScore;

    @Column(nullable = true)
    private String locationLabel;
}
