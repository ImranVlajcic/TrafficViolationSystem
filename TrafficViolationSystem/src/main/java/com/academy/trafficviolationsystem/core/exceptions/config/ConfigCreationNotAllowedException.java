package com.academy.trafficviolationsystem.core.exceptions.config;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class ConfigCreationNotAllowedException extends AppException {
    public ConfigCreationNotAllowedException() {
        super(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.CONFIG_CREATION_NOT_ALLOWED,
                "SystemConfig rows are seeded by Flyway and cannot be created via HTTP");
    }
}
