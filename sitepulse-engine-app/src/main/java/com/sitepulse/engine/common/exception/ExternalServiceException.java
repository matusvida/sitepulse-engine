package com.sitepulse.engine.common.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends SitePulseException {

    public ExternalServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, "external_service_error", message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "external_service_error", message, cause);
    }
}
