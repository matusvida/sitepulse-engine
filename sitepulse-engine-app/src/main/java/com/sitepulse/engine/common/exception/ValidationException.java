package com.sitepulse.engine.common.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends SitePulseException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "validation_error", message);
    }
}
