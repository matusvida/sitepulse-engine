package com.sitepulse.engine.detection.application.service;

public abstract class OpenAiRetryException extends RuntimeException {

    private final String failureReason;
    private final RuntimeException original;

    protected OpenAiRetryException(String failureReason, RuntimeException original) {
        super(original.getMessage(), original);
        this.failureReason = failureReason;
        this.original = original;
    }

    public String failureReason() {
        return failureReason;
    }

    public RuntimeException original() {
        return original;
    }
}
