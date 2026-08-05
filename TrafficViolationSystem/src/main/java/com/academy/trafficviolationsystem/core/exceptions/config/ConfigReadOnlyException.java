package com.academy.trafficviolationsystem.core.exceptions.config;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class ConfigReadOnlyException extends AppException {
    public ConfigReadOnlyException(String configKey) {
        super(HttpStatus.FORBIDDEN, ErrorCode.CONFIG_READ_ONLY,
                "Config key '" + configKey + "' is read-only and cannot be modified");
    }
}
