package com.sitepulse.engine.detection.application.service;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
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

        assertEquals(DetectionFailureReason.OPENAI_RATE_LIMITED, DetectionExecutionService.failureReason(ex));
        assertTrue(DetectionExecutionService.toRetryException(ex) instanceof OpenAiRetryableException);
    }

    @Test
    void failureReasonClassifiesOpenAiServerError() {
        RuntimeException ex = feignException(503, "Service Unavailable");

        assertEquals(DetectionFailureReason.OPENAI_SERVER_ERROR, DetectionExecutionService.failureReason(ex));
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

        assertEquals(DetectionFailureReason.OPENAI_TIMEOUT, DetectionExecutionService.failureReason(ex));
        assertTrue(DetectionExecutionService.toRetryException(ex) instanceof OpenAiRetryableException);
    }

    @Test
    void failureReasonClassifiesLocalRateLimiterRejection() {
        RuntimeException ex = RequestNotPermitted.createRequestNotPermitted(
                io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("openaiDetection")
        );

        assertEquals(DetectionFailureReason.OPENAI_LOCAL_RATE_LIMITED, DetectionExecutionService.failureReason(ex));
        assertTrue(DetectionExecutionService.toRetryException(ex) instanceof OpenAiNonRetryableException);
    }

    @Test
    void failureReasonClassifiesBulkheadRejection() {
        RuntimeException ex = BulkheadFullException.createBulkheadFullException(
                io.github.resilience4j.bulkhead.Bulkhead.ofDefaults("openaiDetection")
        );

        assertEquals(DetectionFailureReason.OPENAI_BULKHEAD_REJECTED, DetectionExecutionService.failureReason(ex));
        assertTrue(DetectionExecutionService.toRetryException(ex) instanceof OpenAiNonRetryableException);
    }

    @Test
    void failureReasonKeepsParseErrorsNonRetryable() {
        RuntimeException ex = new IllegalStateException("Failed to parse OpenAI detection response");

        assertEquals(DetectionFailureReason.PARSE_ERROR, DetectionExecutionService.failureReason(ex));
        assertTrue(DetectionExecutionService.toRetryException(ex) instanceof OpenAiNonRetryableException);
    }

    @Test
    void failureReasonClassifiesBadRequestWithoutRetry() {
        RuntimeException ex = feignException(400, "Bad Request");

        assertEquals(DetectionFailureReason.OPENAI_BAD_REQUEST, DetectionExecutionService.failureReason(ex));
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
