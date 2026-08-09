package com.academy.trafficviolationsystem.rodezone;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.validator.constraints.Range;

@Data @NoArgsConstructor @AllArgsConstructor
public class RoadZoneUpdateRequest {

    private String name;

    private ZoneType zoneType;

    @Min(value = 5, message = "Speed limit must be at least 5 km/h")
    private Integer speedLimitKmh;

    private String description;

    /** null = leave unchanged; false = deactivate zone */
    private Boolean isActive;

    @Range(min = -90, max = 90, message = "Latitude must be between -90 and 90")
    private Double centerLatitude;

    @Range(min = -180, max = 180, message = "Longitude must be between -180 and 180")
    private Double centerLongitude;

    @Positive(message = "Radius must be a positive number")
    private Integer radiusMeters;

    private String geoJsonBoundary;

    @AssertTrue(message = "Zone must have either a circular shape or a polygon, but not both.")
    public boolean isShapeValid() {
        boolean noShapeFields =
                radiusMeters == null
                        && centerLatitude == null
                        && centerLongitude == null
                        && geoJsonBoundary == null;

        if (noShapeFields) {
            return true;
        }

        boolean hasCompleteCircle =
                radiusMeters != null
                        && centerLatitude != null
                        && centerLongitude != null;

        boolean hasPolygon =
                geoJsonBoundary != null
                        && !geoJsonBoundary.isBlank();

        return hasCompleteCircle ^ hasPolygon;
    }
}
