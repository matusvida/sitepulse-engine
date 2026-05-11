package com.sitepulse.engine.common.web;

import com.sitepulse.engine.common.exception.SitePulseException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
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

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "forbidden",
                "You do not have access to this resource",
                request.getRequestURI(),
                List.of()
        ));
    }
}
