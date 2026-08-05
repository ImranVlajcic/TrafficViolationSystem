package com.academy.trafficviolationsystem.configuration;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigSearchObject extends BaseSearchObject<Integer> {

    /** Filter by grouping label: FINE, DRIVER, NOTIFICATION, PDF, MQTT */
    private String category;

    /** Filter by storage type */
    private ConfigDataType dataType;
}
