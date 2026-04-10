package com.sitepulse.engine.detection.application.service;

import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.model.DetectionContext;
import com.sitepulse.engine.detection.domain.model.DetectionHealth;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.DetectionInference;
import com.sitepulse.engine.detection.domain.model.DetectionProvider;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.detection.domain.port.DetectionGateway;
import com.sitepulse.engine.detection.infrastructure.external.openai.OpenAiDetectionGateway;
import com.sitepulse.engine.detection.infrastructure.external.openai.OpenAiDetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionExecutionService {

    private final SitePulseProperties properties;
    private final DetectionGateway yoloDetectionGateway;
    private final OpenAiDetectionGateway openAiDetectionGateway;
    private final DetectionContextService detectionContextService;
    private final DetectionAnalysisRunService detectionAnalysisRunService;
    private final CameraLookup cameraLookup;

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
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                OpenAiDetectionResult result = openAiDetectionGateway.infer(imageBytes, context, cameraSettings);
                Integer runId = detectionAnalysisRunService.recordRun(
                        image.getId(),
                        previousImageId,
                        DetectionProvider.OPENAI.name().toLowerCase(),
                        result.inference().modelVersion(),
                        result.promptVersion(),
                        attempt - 1,
                        "success",
                        result.inference().inferenceMs(),
                        null,
                        result.rawResponse()
                );
                log.info(
                        "OpenAI detection succeeded imageId={} prompt_version={} attempt={} roi_in_prompt={} detections={}",
                        image.getId(),
                        result.promptVersion(),
                        attempt,
                        result.roiIncluded(),
                        result.inference().rawDetections().size()
                );
                return new DetectionExecutionResult(DetectionProvider.OPENAI, result.inference(), runId);
            } catch (RuntimeException ex) {
                String failureReason = failureReason(ex);
                detectionAnalysisRunService.recordRun(
                        image.getId(),
                        previousImageId,
                        DetectionProvider.OPENAI.name().toLowerCase(),
                        properties.openaiModel(),
                        openAiDetectionGateway.promptVersion(),
                        attempt - 1,
                        "failed",
                        null,
                        failureReason + ": " + ex.getMessage(),
                        null
                );
                log.warn(
                        "OpenAI detection failed imageId={} prompt_version={} attempt={} reason={}",
                        image.getId(),
                        openAiDetectionGateway.promptVersion(),
                        attempt,
                        failureReason
                );
                if (attempt == 3) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("OpenAI detection failed unexpectedly");
    }

    private DetectionExecutionResult runYolo(DetectionImage image, byte[] imageBytes, Integer previousImageId) {
        DetectionInference inference = yoloDetectionGateway.infer(imageBytes);
        Integer runId = detectionAnalysisRunService.recordRun(
                image.getId(),
                previousImageId,
                DetectionProvider.YOLO.name().toLowerCase(),
                inference.modelVersion(),
                null,
                0,
                "success",
                inference.inferenceMs(),
                null,
                null
        );
        return new DetectionExecutionResult(DetectionProvider.YOLO, inference, runId);
    }

    private String failureReason(RuntimeException ex) {
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
}
