package com.academy.trafficviolationsystem.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SystemConfigUpdateRequest {

    @NotBlank(message = "configValue must not be blank")
    private String configValue;

    private String description;
}
