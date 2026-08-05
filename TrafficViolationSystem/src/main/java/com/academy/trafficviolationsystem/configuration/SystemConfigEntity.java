package com.academy.trafficviolationsystem.configuration;

import com.academy.trafficviolationsystem.core.entities.AutoIdBaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "system_config",
    indexes = {
        @Index(name = "idx_sysconf_key", columnList = "config_key"),
        @Index(name = "idx_sysconf_cat", columnList = "category")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SystemConfigEntity extends AutoIdBaseEntity {

    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigDataType dataType;

    /**
     * Grouping label: FINE, DRIVER, NOTIFICATION, PDF, MQTT
     */
    @Column(nullable = false)
    private String category;

    @Column(nullable = true)
    private String description;

    /**
     * false = read-only system constant; cannot be changed via HTTP.
     */
    @Column(nullable = false)
    private boolean isEditable = true;
}
