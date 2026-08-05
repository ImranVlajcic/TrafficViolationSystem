package com.academy.trafficviolationsystem.configuration;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SystemConfigDto {

    private Integer id;
    private String configKey;
    private String configValue;
    private ConfigDataType dataType;
    private String category;
    private String description;
    private boolean isEditable;
}
