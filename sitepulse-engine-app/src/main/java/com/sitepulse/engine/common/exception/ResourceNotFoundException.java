package com.sitepulse.engine.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends SitePulseException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "resource_not_found", message);
    }
}
