package com.sitepulse.engine.detection.application.service;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionExecutionServiceTest {

    @Test
    void failureReasonClassifiesOpenAiRateLimit() {
        RuntimeException ex = feignException(429, "Too Many Requests");

        assertEquals("openai_rate_limited", DetectionExecutionService.failureReason(ex));
        assertTrue(DetectionExecutionService.toRetryException(ex) instanceof OpenAiRetryableException);
    }

    @Test
    void failureReasonClassifiesOpenAiServerError() {
        RuntimeException ex = feignException(503, "Service Unavailable");

        assertEquals("openai_server_error", DetectionExecutionService.failureReason(ex));
        assertTrue(DetectionExecutionService.toRetryException(ex) instanceof OpenAiRetryableException);
    }

    @Test
    void failureReasonClassifiesTimeoutsAsRetryable() {
        RuntimeException ex = new RetryableException(
                408,
                "Read timed out",
                Request.HttpMethod.POST,
                new java.net.SocketTimeoutException("Read timed out"),
                new Date(),
                request()
        );

        assertEquals("openai_timeout", DetectionExecutionService.failureReason(ex));
        assertTrue(DetectionExecutionService.toRetryException(ex) instanceof OpenAiRetryableException);
    }

    @Test
    void failureReasonKeepsParseErrorsNonRetryable() {
        RuntimeException ex = new IllegalStateException("Failed to parse OpenAI detection response");

        assertEquals("parse_error", DetectionExecutionService.failureReason(ex));
        assertTrue(DetectionExecutionService.toRetryException(ex) instanceof OpenAiNonRetryableException);
    }

    @Test
    void failureReasonClassifiesBadRequestWithoutRetry() {
        RuntimeException ex = feignException(400, "Bad Request");

        assertEquals("openai_bad_request", DetectionExecutionService.failureReason(ex));
        assertTrue(DetectionExecutionService.toRetryException(ex) instanceof OpenAiNonRetryableException);
    }

    private static FeignException feignException(int status, String reason) {
        return FeignException.errorStatus(
                "openAiChat",
                Response.builder()
                        .status(status)
                        .reason(reason)
                        .request(request())
                        .headers(Map.of())
                        .body(reason, StandardCharsets.UTF_8)
                        .build()
        );
    }

    private static Request request() {
        return Request.create(
                Request.HttpMethod.POST,
                "https://api.openai.com/v1/chat/completions",
                Map.of(),
                Request.Body.create("{}", StandardCharsets.UTF_8),
                null
        );
    }
}
