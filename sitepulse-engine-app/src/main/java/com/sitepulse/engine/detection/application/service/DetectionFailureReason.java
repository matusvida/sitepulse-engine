package com.sitepulse.engine.detection.application.service;

public enum DetectionFailureReason {
    OPENAI_LOCAL_RATE_LIMITED("openai_local_rate_limited"),
    OPENAI_BULKHEAD_REJECTED("openai_bulkhead_rejected"),
    OPENAI_RATE_LIMITED("openai_rate_limited"),
    OPENAI_BAD_REQUEST("openai_bad_request"),
    OPENAI_AUTH_ERROR("openai_auth_error"),
    OPENAI_SERVER_ERROR("openai_server_error"),
    OPENAI_TIMEOUT("openai_timeout"),
    PARSE_ERROR("parse_error"),
    UNKNOWN_CLASS("unknown_class"),
    IMAGE_DECODE_ERROR("image_decode_error"),
    RUNTIME_ERROR("runtime_error");

    private final String value;

    DetectionFailureReason(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
