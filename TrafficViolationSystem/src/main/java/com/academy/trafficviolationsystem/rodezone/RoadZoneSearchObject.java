package com.academy.trafficviolationsystem.rodezone;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class RoadZoneSearchObject extends BaseSearchObject<Integer> {

    /** Filter by zone type */
    private ZoneType zoneType;

    /** Filter by active flag — null means return all */
    private Boolean isActive;

    /** Partial match on zone name (case-insensitive LIKE) */
    private String search;
}
