package com.sitepulse.engine.detection.application.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;

@Component
public class OpenAiRetryExecutor {

    @Bulkhead(name = "openaiDetection", type = Bulkhead.Type.SEMAPHORE)
    @RateLimiter(name = "openaiDetection")
    @Retry(name = "openaiDetection")
    public <T> T execute(Callable<T> operation) {
        try {
            return operation.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unexpected checked exception during OpenAI detection", ex);
        }
    }
}
