package com.sitepulse.engine.common.exception;

import org.springframework.http.HttpStatus;

public abstract class SitePulseException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected SitePulseException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    protected SitePulseException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
