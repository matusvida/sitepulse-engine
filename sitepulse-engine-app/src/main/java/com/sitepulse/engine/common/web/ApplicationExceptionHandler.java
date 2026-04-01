package com.sitepulse.engine.common.web;

import com.sitepulse.engine.common.exception.SitePulseException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ApplicationExceptionHandler {

    @ExceptionHandler(SitePulseException.class)
    public ResponseEntity<ApiErrorResponse> handleSitePulseException(SitePulseException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus()).body(new ApiErrorResponse(
                OffsetDateTime.now(),
                exception.getStatus().value(),
                exception.getStatus().getReasonPhrase(),
                exception.getCode(),
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        ));
    }
}
