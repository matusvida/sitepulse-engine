package com.sitepulse.engine.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "validation_error",
                "Validation failed",
                request.getRequestURI(),
                violations
        ));
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiErrorResponse> handleRequestValidation(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "validation_error",
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        ));
    }

    private ApiViolation toViolation(FieldError error) {
        return new ApiViolation(error.getField(), error.getDefaultMessage());
    }
}
