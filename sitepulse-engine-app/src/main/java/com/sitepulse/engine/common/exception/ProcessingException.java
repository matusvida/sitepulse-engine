package com.sitepulse.engine.common.exception;

import org.springframework.http.HttpStatus;

public class ProcessingException extends SitePulseException {

    public ProcessingException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "processing_error", message);
    }

    public ProcessingException(String message, Throwable cause) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "processing_error", message, cause);
    }
}
