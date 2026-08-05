package com.academy.trafficviolationsystem.core.exceptions.infrastructure;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class MqttConnectionException extends AppException {
    public MqttConnectionException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.MQTT_CONNECTION_ERROR,
                "MQTT broker connection error: " + detail);
    }
}
