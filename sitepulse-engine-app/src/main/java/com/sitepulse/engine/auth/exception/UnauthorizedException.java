package com.sitepulse.engine.auth.exception;

import com.sitepulse.engine.common.exception.SitePulseException;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends SitePulseException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "unauthorized", message);
    }
}
