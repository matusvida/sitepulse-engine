package com.sitepulse.engine.detection.application.service;

public class OpenAiRetryableException extends OpenAiRetryException {

    public OpenAiRetryableException(String failureReason, RuntimeException original) {
        super(failureReason, original);
    }
}
