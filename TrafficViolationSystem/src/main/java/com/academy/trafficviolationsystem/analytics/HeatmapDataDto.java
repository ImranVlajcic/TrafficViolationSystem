package com.academy.trafficviolationsystem.analytics;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HeatmapDataDto {
    private Double latitude;
    private Double longitude;
    private int violationCount;
    private Double severityScore;
    private String dominantType;
    private String locationLabel;
}
