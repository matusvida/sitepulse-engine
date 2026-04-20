package com.sitepulse.engine.detection.application.service;

import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.model.DetectionContext;
import com.sitepulse.engine.detection.domain.model.DetectionHealth;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.DetectionInference;
import com.sitepulse.engine.detection.domain.model.AiDetectionResult;
import com.sitepulse.engine.detection.domain.enums.DetectionProvider;
import com.sitepulse.engine.detection.domain.port.AiDetectionGateway;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.detection.domain.port.DetectionGateway;
import feign.FeignException;
import feign.RetryableException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionExecutionService {

    private final SitePulseProperties properties;
    private final DetectionGateway yoloDetectionGateway;
    private final AiDetectionGateway aiDetectionGateway;
    private final DetectionContextService detectionContextService;
    private final DetectionAnalysisRunService detectionAnalysisRunService;
    private final CameraLookup cameraLookup;
    private final OpenAiRetryExecutor openAiRetryExecutor;

    public DetectionExecutionResult execute(DetectionImage image, byte[] imageBytes) {
        CameraRoiSettings cameraSettings = image.getProjectId() == null ? null : cameraLookup.findRoiSettings(image.getProjectId(), image.getKey());
        return execute(image, imageBytes, cameraSettings);
    }

    public DetectionExecutionResult execute(DetectionImage image, byte[] imageBytes, CameraRoiSettings cameraSettings) {
        DetectionProvider provider = DetectionProvider.from(properties.detectionProvider());
        Integer previousImageId = detectionContextService.findPreviousContext(image).map(DetectionContext::imageId).orElse(null);
        if (provider == DetectionProvider.YOLO) {
            return runYolo(image, imageBytes, previousImageId);
        }
        return runOpenAi(image, imageBytes, previousImageId, cameraSettings);
    }

    public DetectionHealth health() {
        DetectionProvider provider = DetectionProvider.from(properties.detectionProvider());
        if (provider == DetectionProvider.YOLO) {
            return yoloDetectionGateway.health();
        }
        boolean loaded = properties.openaiApiKey() != null && !properties.openaiApiKey().isBlank();
        return new DetectionHealth(loaded ? "ok" : "down", loaded, properties.openaiModel());
    }

    private DetectionExecutionResult runOpenAi(DetectionImage image, byte[] imageBytes, Integer previousImageId, CameraRoiSettings cameraSettings) {
        DetectionContext context = detectionContextService.findPreviousContext(image).orElse(null);
        AtomicInteger attemptCounter = new AtomicInteger();
        try {
            return openAiRetryExecutor.execute(() -> runOpenAiAttempt(
                    image,
                    imageBytes,
                    previousImageId,
                    cameraSettings,
                    context,
                    attemptCounter.incrementAndGet()
            ));
        } catch (OpenAiRetryException ex) {
            throw ex.original();
        }
    }

    private DetectionExecutionResult runYolo(DetectionImage image, byte[] imageBytes, Integer previousImageId) {
        DetectionInference inference = yoloDetectionGateway.infer(imageBytes);
        Integer runId = detectionAnalysisRunService.startRun(
                image.getId(),
                previousImageId,
                DetectionProvider.YOLO.name().toLowerCase(),
                inference.modelVersion(),
                null,
                0
        );
        return new DetectionExecutionResult(DetectionProvider.YOLO, inference, runId, null);
    }

    private DetectionExecutionResult runOpenAiAttempt(
            DetectionImage image,
            byte[] imageBytes,
            Integer previousImageId,
            CameraRoiSettings cameraSettings,
            DetectionContext context,
            int attempt
    ) {
        Integer runId = detectionAnalysisRunService.startRun(
                image.getId(),
                previousImageId,
                DetectionProvider.OPENAI.name().toLowerCase(),
                properties.openaiModel(),
                aiDetectionGateway.promptVersion(),
                attempt - 1
        );
        try {
            AiDetectionResult result = aiDetectionGateway.infer(imageBytes, context, cameraSettings);
            log.info(
                    "OpenAI detection succeeded imageId={} prompt_version={} attempt={} roi_in_prompt={} detections={}",
                    image.getId(),
                    result.promptVersion(),
                    attempt,
                    result.roiIncluded(),
                    result.inference().rawDetections().size()
            );
            return new DetectionExecutionResult(DetectionProvider.OPENAI, result.inference(), runId, result.rawResponse());
        } catch (RuntimeException ex) {
            OpenAiRetryException mapped = toRetryException(ex);
            detectionAnalysisRunService.completeFailure(runId, mapped.failureReason() + ": " + mapped.getMessage());
            log.warn(
                    "OpenAI detection failed imageId={} prompt_version={} attempt={} reason={} exception={} message={} retryable={}",
                    image.getId(),
                    aiDetectionGateway.promptVersion(),
                    attempt,
                    mapped.failureReason(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    mapped instanceof OpenAiRetryableException
            );
            throw mapped;
        }
    }

    static String failureReason(RuntimeException ex) {
        Integer status = statusCode(ex);
        if (status != null) {
            if (status == 429) {
                return "openai_rate_limited";
            }
            if (status == 400) {
                return "openai_bad_request";
            }
            if (status == 401 || status == 403) {
                return "openai_auth_error";
            }
            if (status >= 500) {
                return "openai_server_error";
            }
        }
        if (isTimeoutLike(ex)) {
            return "openai_timeout";
        }
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "runtime_error";
        }
        String message = ex.getMessage().toLowerCase();
        if (message.contains("parse")) {
            return "parse_error";
        }
        if (message.contains("class_id") || message.contains("class_name")) {
            return "unknown_class";
        }
        if (message.contains("decode")) {
            return "image_decode_error";
        }
        return "runtime_error";
    }

    static OpenAiRetryException toRetryException(RuntimeException ex) {
        return switch (failureReason(ex)) {
            case "openai_rate_limited", "openai_server_error", "openai_timeout" -> new OpenAiRetryableException(failureReason(ex), ex);
            default -> new OpenAiNonRetryableException(failureReason(ex), ex);
        };
    }

    private static Integer statusCode(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof FeignException feignException) {
                return feignException.status();
            }
            current = current.getCause();
        }
        return null;
    }

    private static boolean isTimeoutLike(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RetryableException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("timeout")
                        || normalized.contains("timed out")
                        || normalized.contains("read timed out")
                        || normalized.contains("connect timed out")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
