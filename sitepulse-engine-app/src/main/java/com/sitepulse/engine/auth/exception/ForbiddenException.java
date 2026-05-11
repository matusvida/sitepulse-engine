package com.sitepulse.engine.auth.exception;

import com.sitepulse.engine.common.exception.SitePulseException;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends SitePulseException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "forbidden", message);
    }
}
