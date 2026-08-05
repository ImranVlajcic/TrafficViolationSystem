package com.academy.trafficviolationsystem.camera;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

/**
 * Search/filter parameters for GET /api/cameras.
 */
@Getter
@Setter
public class CameraSearchObject extends BaseSearchObject<Integer> {

    /** Free-text search on name, serialNumber, locationDescription. */
    private String search;

    private CameraType cameraType;

    /** null = all, true = only online, false = only offline. */
    private Boolean isOnline;

    /** null = all, true = only active, false = only decommissioned. */
    private Boolean isActive;
}
