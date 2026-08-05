package com.academy.trafficviolationsystem.core.exceptions.config;

import com.academy.trafficviolationsystem.configuration.ConfigDataType;
import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class ConfigTypeMismatchException extends AppException {
    public ConfigTypeMismatchException(String key, ConfigDataType actual, ConfigDataType requested) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.CONFIG_TYPE_MISMATCH,
                "Config key '" + key + "' has dataType " + actual +
                        " but was requested as " + requested);
    }
}
