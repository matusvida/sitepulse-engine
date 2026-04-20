package com.sitepulse.engine.detection.application.service;

public class OpenAiNonRetryableException extends OpenAiRetryException {

    public OpenAiNonRetryableException(DetectionFailureReason failureReason, RuntimeException original) {
        super(failureReason, original);
    }
}
