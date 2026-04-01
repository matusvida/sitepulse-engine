package com.sitepulse.engine.common.exception;

import org.springframework.http.HttpStatus;

public class ConfigurationException extends SitePulseException {

    public ConfigurationException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "configuration_error", message);
    }
}
