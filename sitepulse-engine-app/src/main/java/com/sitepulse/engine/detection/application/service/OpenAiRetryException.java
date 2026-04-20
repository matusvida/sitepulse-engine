package com.sitepulse.engine.detection.application.service;

public abstract class OpenAiRetryException extends RuntimeException {

    private final DetectionFailureReason failureReason;
    private final RuntimeException original;

    protected OpenAiRetryException(DetectionFailureReason failureReason, RuntimeException original) {
        super(original.getMessage(), original);
        this.failureReason = failureReason;
        this.original = original;
    }

    public DetectionFailureReason failureReason() {
        return failureReason;
    }

    public RuntimeException original() {
        return original;
    }
}
