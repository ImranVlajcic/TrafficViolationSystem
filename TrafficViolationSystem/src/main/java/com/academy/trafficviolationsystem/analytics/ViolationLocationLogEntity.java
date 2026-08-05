package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "violation_location_log",
    indexes = {
        @Index(name = "idx_vll_location", columnList = "latitude, longitude"),
        @Index(name = "idx_vll_occurred", columnList = "occurred_at DESC")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ViolationLocationLogEntity extends UUIDBaseEntity {

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String violationType;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /**
     * Raw FK column — no @ManyToOne to keep this table truly write-once
     * and avoid any lazy-load overhead in heatmap queries.
     */
    @Column(name = "violation_id", nullable = false, unique = true,
            columnDefinition = "uuid")
    private UUID violationId;
}
