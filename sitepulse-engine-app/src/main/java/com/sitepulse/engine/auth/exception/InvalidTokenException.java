package com.sitepulse.engine.auth.exception;

import com.sitepulse.engine.common.exception.SitePulseException;
import org.springframework.http.HttpStatus;

public class InvalidTokenException extends SitePulseException {

    public InvalidTokenException(String message) {
        super(HttpStatus.BAD_REQUEST, "invalid_token", message);
    }
}
